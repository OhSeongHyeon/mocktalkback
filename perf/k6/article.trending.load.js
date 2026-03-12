import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
  authJsonParams,
  authParams,
  resolveApiBaseUrl,
  selectAccessToken,
  setupAuth,
} from './lib/k6-auth.js';

const API_BASE_URL = resolveApiBaseUrl();
const ARTICLE_ID = Number(__ENV.K6_ARTICLE_ID || 1);
const TREND_LIMIT = Number(__ENV.K6_TREND_LIMIT || 10);

export const article_trending_failures = new Rate('article_trending_failures');
export const article_trending_requests = new Counter('article_trending_requests');

export const options = {
  scenarios: {
    article_event_mix: {
      executor: 'constant-vus',
      exec: 'articleEventMixScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_TREND_EVENT_MIX || 3),
      duration: __ENV.K6_DURATION_ARTICLE_TREND_EVENT_MIX || '30s',
    },
    article_trending_read: {
      executor: 'constant-vus',
      exec: 'articleTrendingReadScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_TREND_READ || 2),
      duration: __ENV.K6_DURATION_ARTICLE_TREND_READ || '30s',
      startTime: __ENV.K6_START_ARTICLE_TREND_READ || '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    article_trending_failures: ['rate==0'],
  },
};

export function setup() {
  // Given: 혼합 이벤트 시나리오가 활성화되어 있으면 로그인 정보가 준비되어 있다.
  if (!requiresAuthenticatedUsers()) {
    return {};
  }

  // When: 로그인 API를 호출해 테스트용 AccessToken 풀을 발급받는다.
  const authData = setupAuth(API_BASE_URL);

  // Then: 혼합 이벤트용 VU 수보다 계정 수가 부족하면 즉시 중단한다.
  validateEventMixCapacity(authData.accessTokens.length);
  return authData;
}

export function articleEventMixScenario(data) {
  // Given: 같은 게시글을 대상으로 조회/반응/북마크/댓글 이벤트를 혼합한다.
  const accessToken = selectAccessToken(data);
  const dice = Math.random();

  // When: 확률에 따라 하나의 이벤트를 실행한다.
  if (dice < 0.35) {
    executeDetailRead(accessToken);
  } else if (dice < 0.60) {
    executeReactionToggle(accessToken);
  } else if (dice < 0.80) {
    executeBookmarkRoundTrip(accessToken);
  } else {
    executeCommentRoundTrip(accessToken);
  }

  // Then: 다음 요청까지 짧게 대기한다.
  sleep(0.2);
}

export function articleTrendingReadScenario() {
  // Given: 공개 인기글 API 조회 요청이 준비되어 있다.
  const window = Math.random() < 0.5 ? 'DAY' : 'WEEK';

  // When: 인기글 API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/articles/trending?window=${window}&limit=${TREND_LIMIT}`
  );

  // Then: 응답 상태와 리스트 구조를 검증한다.
  article_trending_requests.add(1);
  const ok = check(response, {
    'article trending status 200': (r) => r.status === 200,
    'article trending envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article trending payload valid': (r) => {
      const body = r.json();
      if (!body || !Array.isArray(body.data)) {
        return false;
      }
      if (body.data.length === 0) {
        return true;
      }
      const first = body.data[0];
      return (
        typeof first.articleId === 'number' &&
        typeof first.title === 'string' &&
        typeof first.trendScore === 'number'
      );
    },
  });
  article_trending_failures.add(!ok);
  sleep(0.2);
}

function executeDetailRead(accessToken) {
  const response = http.get(`${API_BASE_URL}/articles/${ARTICLE_ID}`, authParams(accessToken));
  article_trending_requests.add(1);
  const ok = check(response, {
    'article mix detail status 200': (r) => r.status === 200,
    'article mix detail envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article mix detail payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.id === ARTICLE_ID;
    },
  });
  article_trending_failures.add(!ok);
}

function executeReactionToggle(accessToken) {
  const reactionType = Math.random() < 0.5 ? 1 : -1;
  const response = http.post(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(accessToken)
  );
  article_trending_requests.add(1);
  const ok = check(response, {
    'article mix reaction status 200': (r) => r.status === 200,
    'article mix reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article mix reaction payload valid': (r) => {
      const body = r.json();
      if (!body || !body.data) {
        return false;
      }
      const summary = body.data;
      return (
        (summary.myReaction === -1 || summary.myReaction === 0 || summary.myReaction === 1) &&
        summary.likeCount >= 0 &&
        summary.dislikeCount >= 0
      );
    },
  });
  article_trending_failures.add(!ok);
}

function executeBookmarkRoundTrip(accessToken) {
  const createResponse = http.post(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/bookmark`,
    null,
    authParams(accessToken)
  );
  article_trending_requests.add(1);
  const createOk = check(createResponse, {
    'article mix bookmark create status 200': (r) => r.status === 200,
    'article mix bookmark create envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article mix bookmark create payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.articleId === ARTICLE_ID;
    },
  });
  article_trending_failures.add(!createOk);
  if (!createOk) {
    return;
  }

  const deleteResponse = http.del(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/bookmark`,
    null,
    authParams(accessToken)
  );
  article_trending_requests.add(1);
  const deleteOk = check(deleteResponse, {
    'article mix bookmark delete status 200': (r) => r.status === 200,
    'article mix bookmark delete envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article mix bookmark delete payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.articleId === ARTICLE_ID;
    },
  });
  article_trending_failures.add(!deleteOk);
}

function executeCommentRoundTrip(accessToken) {
  const content = `k6 comment vu=${Number(__VU || 0)} iter=${Number(__ITER || 0)}`;
  const createResponse = http.post(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/comments`,
    JSON.stringify({ content }),
    authJsonParams(accessToken)
  );
  article_trending_requests.add(1);
  const createOk = check(createResponse, {
    'article mix comment create status 200': (r) => r.status === 200,
    'article mix comment create envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article mix comment create payload valid': (r) => {
      const body = r.json();
      return body && body.data && typeof body.data.id === 'number';
    },
  });
  article_trending_failures.add(!createOk);
  if (!createOk) {
    return;
  }

  const commentId = createResponse.json().data.id;
  const deleteResponse = http.del(
    `${API_BASE_URL}/comments/${commentId}`,
    null,
    authParams(accessToken)
  );
  article_trending_requests.add(1);
  const deleteOk = check(deleteResponse, {
    'article mix comment delete status 200': (r) => r.status === 200,
    'article mix comment delete envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
  });
  article_trending_failures.add(!deleteOk);
}

function requiresAuthenticatedUsers() {
  return Number(__ENV.K6_VUS_ARTICLE_TREND_EVENT_MIX || 3) > 0;
}

function validateEventMixCapacity(poolSize) {
  const eventMixVus = Number(__ENV.K6_VUS_ARTICLE_TREND_EVENT_MIX || 3);
  if (eventMixVus > 0 && poolSize < eventMixVus) {
    throw new Error(
      `혼합 이벤트 시나리오용 계정 수가 부족합니다. required=${eventMixVus}, actual=${poolSize}`
    );
  }
}
