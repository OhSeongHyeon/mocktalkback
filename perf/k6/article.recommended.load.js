import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
  authParams,
  loginAndGetAccessToken,
  requireLoginEnv,
  resolveApiBaseUrl,
} from './lib/k6-auth.js';
import { createSummaryHandler } from './lib/k6-summary.js';

const API_BASE_URL = resolveApiBaseUrl();
const RECOMMENDED_LIMIT = Number(__ENV.K6_RECOMMEND_LIMIT || 9);
const RECENT_PAGE = Number(__ENV.K6_RECOMMEND_HOME_PAGE || 0);
const RECENT_SIZE = Number(__ENV.K6_RECOMMEND_HOME_RECENT_SIZE || 15);
const TREND_WINDOW = __ENV.K6_RECOMMEND_HOME_TREND_WINDOW || 'DAY';
const TREND_LIMIT = Number(__ENV.K6_RECOMMEND_HOME_TREND_LIMIT || 9);

export const article_recommended_failures = new Rate('article_recommended_failures');
export const article_recommended_requests = new Counter('article_recommended_requests');

export const options = {
  scenarios: buildScenarios(),
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    article_recommended_failures: ['rate==0'],
  },
};

export const handleSummary = createSummaryHandler('article-recommended');

export function setup() {
  const warmScenarioEnabled = requiresWarmUsers();
  const coldScenarioEnabled = requiresColdUsers();

  const warmCredentials = warmScenarioEnabled
    ? resolveCredentialPoolFromEnv('K6_RECOMMEND_WARM_USERS', true)
    : [];
  const coldCredentials = coldScenarioEnabled
    ? resolveCredentialPoolFromEnv('K6_RECOMMEND_COLD_USERS', false)
    : [];

  const warmAccessTokens = warmCredentials.map((credential) =>
    loginAndGetAccessToken(API_BASE_URL, credential.loginId, credential.password)
  );
  const coldAccessTokens = coldCredentials.map((credential) =>
    loginAndGetAccessToken(API_BASE_URL, credential.loginId, credential.password)
  );

  validatePoolCapacity(
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_WARM || 1),
    warmAccessTokens.length,
    'warm user'
  );
  validatePoolCapacity(
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_HOME || 1),
    warmAccessTokens.length,
    'home journey'
  );
  validatePoolCapacity(
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_COLD || 1),
    coldAccessTokens.length,
    'cold user'
  );

  return {
    warmAccessTokens,
    coldAccessTokens,
  };
}

export function guestRecommendationScenario() {
  // Given: 비로그인 사용자가 추천 탭에 진입한다.
  const response = http.get(`${API_BASE_URL}/articles/recommended?limit=${RECOMMENDED_LIMIT}`);

  // Then: fallback 추천 응답 구조와 공개 노출 정책을 검증한다.
  recordRecommendationChecks(response, {
    responseLabel: 'guest recommended',
    expectedPersonalized: false,
  });
  sleep(0.2);
}

export function warmUserRecommendationScenario(data) {
  // Given: 활동 이력이 있는 로그인 사용자가 추천 탭을 연다.
  const accessToken = selectToken(data.warmAccessTokens);
  const response = http.get(
    `${API_BASE_URL}/articles/recommended?limit=${RECOMMENDED_LIMIT}`,
    authParams(accessToken)
  );

  // Then: 추천 응답 구조와 추천 이유 필드를 검증한다.
  recordRecommendationChecks(response, {
    responseLabel: 'warm recommended',
    requireReason: true,
  });
  sleep(0.2);
}

export function coldUserRecommendationScenario(data) {
  // Given: 활동 이력이 적은 로그인 사용자가 추천 탭을 연다.
  const accessToken = selectToken(data.coldAccessTokens);
  const response = http.get(
    `${API_BASE_URL}/articles/recommended?limit=${RECOMMENDED_LIMIT}`,
    authParams(accessToken)
  );

  // Then: fallback 또는 약한 개인화 응답이라도 구조가 유지되는지 검증한다.
  recordRecommendationChecks(response, {
    responseLabel: 'cold recommended',
    requireReason: true,
  });
  sleep(0.2);
}

export function homeTabJourneyScenario(data) {
  // Given: 로그인 사용자가 홈에서 최신글 -> 트렌딩 -> 추천 순으로 전환한다.
  const accessToken = selectToken(data.warmAccessTokens);

  // When: 홈 3탭이 사용하는 API를 순서대로 호출한다.
  const recentResponse = http.get(
    `${API_BASE_URL}/articles/recent?page=${RECENT_PAGE}&size=${RECENT_SIZE}`,
    authParams(accessToken)
  );
  recordRecentChecks(recentResponse);

  const trendingResponse = http.get(
    `${API_BASE_URL}/articles/trending?window=${TREND_WINDOW}&limit=${TREND_LIMIT}`,
    authParams(accessToken)
  );
  recordTrendingChecks(trendingResponse);

  const recommendedResponse = http.get(
    `${API_BASE_URL}/articles/recommended?limit=${RECOMMENDED_LIMIT}`,
    authParams(accessToken)
  );
  recordRecommendationChecks(recommendedResponse, {
    responseLabel: 'home recommended',
    requireReason: true,
  });

  // Then: 탭 전환 직후 재호출 간격을 짧게 둔다.
  sleep(0.2);
}

function recordRecommendationChecks(response, options = {}) {
  article_recommended_requests.add(1);
  const responseLabel = options.responseLabel || 'recommended';
  const ok = check(response, {
    [`${responseLabel} status 200`]: (r) => r.status === 200,
    [`${responseLabel} envelope success`]: (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    [`${responseLabel} payload valid`]: (r) => {
      const body = r.json();
      if (!body || !Array.isArray(body.data)) {
        return false;
      }
      if (body.data.length > RECOMMENDED_LIMIT) {
        return false;
      }
      if (body.data.length === 0) {
        return true;
      }

      return body.data.every((item) => (
        typeof item.articleId === 'number' &&
        typeof item.boardSlug === 'string' &&
        item.boardSlug !== 'notice' &&
        item.boardSlug !== 'inquiry' &&
        typeof item.title === 'string' &&
        typeof item.recommendationScore === 'number' &&
        typeof item.personalized === 'boolean' &&
        (!options.requireReason || typeof item.recommendationReason === 'string')
      ));
    },
    [`${responseLabel} personalized flag valid`]: (r) => {
      if (options.expectedPersonalized === undefined) {
        return true;
      }
      const body = r.json();
      if (!body || !Array.isArray(body.data) || body.data.length === 0) {
        return true;
      }
      return body.data.every((item) => item.personalized === options.expectedPersonalized);
    },
  });
  article_recommended_failures.add(!ok);
}

function recordRecentChecks(response) {
  article_recommended_requests.add(1);
  const ok = check(response, {
    'home recent status 200': (r) => r.status === 200,
    'home recent envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'home recent payload valid': (r) => {
      const body = r.json();
      return body &&
        body.data &&
        Array.isArray(body.data.items) &&
        typeof body.data.hasNext === 'boolean';
    },
  });
  article_recommended_failures.add(!ok);
}

function recordTrendingChecks(response) {
  article_recommended_requests.add(1);
  const ok = check(response, {
    'home trending status 200': (r) => r.status === 200,
    'home trending envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'home trending payload valid': (r) => {
      const body = r.json();
      if (!body || !Array.isArray(body.data)) {
        return false;
      }
      if (body.data.length === 0) {
        return true;
      }
      return body.data.every((item) => (
        typeof item.articleId === 'number' &&
        typeof item.title === 'string' &&
        typeof item.trendScore === 'number'
      ));
    },
  });
  article_recommended_failures.add(!ok);
}

function requiresWarmUsers() {
  return (
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_WARM || 1) > 0 ||
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_HOME || 1) > 0
  );
}

function requiresColdUsers() {
  return Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_COLD || 1) > 0;
}

function validatePoolCapacity(vus, poolSize, label) {
  if (vus <= 0) {
    return;
  }
  if (poolSize < vus) {
    throw new Error(`추천 ${label} 시나리오용 계정 수가 부족합니다. required=${vus}, actual=${poolSize}`);
  }
}

function selectToken(accessTokens) {
  if (!Array.isArray(accessTokens) || accessTokens.length === 0) {
    throw new Error('사용 가능한 추천 시나리오용 AccessToken이 없습니다.');
  }
  const vuNumber = Number(__VU || 1);
  const tokenIndex = Math.max(0, (vuNumber - 1) % accessTokens.length);
  return accessTokens[tokenIndex];
}

function resolveCredentialPoolFromEnv(envName, allowLoginUsersFallback) {
  const directPool = parseCredentialPool(__ENV[envName]);
  if (directPool.length > 0) {
    return directPool;
  }

  if (allowLoginUsersFallback) {
    const loginUsersPool = parseCredentialPool(__ENV.K6_LOGIN_USERS);
    if (loginUsersPool.length > 0) {
      return loginUsersPool;
    }
  }

  return [requireLoginEnv()];
}

function parseCredentialPool(rawValue) {
  if (!rawValue) {
    return [];
  }

  return rawValue
    .split(',')
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const separatorIndex = entry.indexOf(':');
      if (separatorIndex <= 0 || separatorIndex === entry.length - 1) {
        throw new Error(`추천 시나리오 계정 형식이 올바르지 않습니다: ${entry}`);
      }
      return {
        loginId: entry.slice(0, separatorIndex).trim(),
        password: entry.slice(separatorIndex + 1).trim(),
      };
    });
}

function buildScenarios() {
  const scenarios = {};
  addConstantVusScenario(
    scenarios,
    'article_recommended_guest',
    'guestRecommendationScenario',
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_GUEST || 1),
    __ENV.K6_DURATION_ARTICLE_RECOMMENDED_GUEST || '20s'
  );
  addConstantVusScenario(
    scenarios,
    'article_recommended_warm',
    'warmUserRecommendationScenario',
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_WARM || 1),
    __ENV.K6_DURATION_ARTICLE_RECOMMENDED_WARM || '20s',
    __ENV.K6_START_ARTICLE_RECOMMENDED_WARM || '25s'
  );
  addConstantVusScenario(
    scenarios,
    'article_recommended_cold',
    'coldUserRecommendationScenario',
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_COLD || 1),
    __ENV.K6_DURATION_ARTICLE_RECOMMENDED_COLD || '20s',
    __ENV.K6_START_ARTICLE_RECOMMENDED_COLD || '50s'
  );
  addConstantVusScenario(
    scenarios,
    'article_recommended_home',
    'homeTabJourneyScenario',
    Number(__ENV.K6_VUS_ARTICLE_RECOMMENDED_HOME || 1),
    __ENV.K6_DURATION_ARTICLE_RECOMMENDED_HOME || '20s',
    __ENV.K6_START_ARTICLE_RECOMMENDED_HOME || '75s'
  );
  return scenarios;
}

function addConstantVusScenario(scenarios, name, execName, vus, duration, startTime) {
  if (vus <= 0) {
    return;
  }
  scenarios[name] = {
    executor: 'constant-vus',
    exec: execName,
    vus,
    duration,
    ...(startTime ? { startTime } : {}),
  };
}
