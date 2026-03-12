import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
  authParams,
  resolveApiBaseUrl,
  selectAccessToken,
  setupAuth,
} from './lib/k6-auth.js';
import {
  fixedAnonymousHeaders,
  fixedIpRotatingUserAgentHeaders,
  rotatingIpFixedUserAgentHeaders,
} from './lib/k6-viewer.js';

const API_BASE_URL = resolveApiBaseUrl();
const ARTICLE_ID = Number(__ENV.K6_ARTICLE_ID || 1);

export const article_view_failures = new Rate('article_view_failures');
export const article_view_requests = new Counter('article_view_requests');

export const options = {
  scenarios: {
    article_same_user_repeat: {
      executor: 'constant-vus',
      exec: 'sameUserRepeatScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_SAME_USER || 1),
      duration: __ENV.K6_DURATION_ARTICLE_SAME_USER || '20s',
    },
    article_multi_user_fan_out: {
      executor: 'per-vu-iterations',
      exec: 'multiUserFanOutScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_MULTI_USER || 5),
      iterations: Number(__ENV.K6_ITERATIONS_ARTICLE_MULTI_USER || 1),
      maxDuration: __ENV.K6_MAX_DURATION_ARTICLE_MULTI_USER || '30s',
      startTime: __ENV.K6_START_ARTICLE_MULTI_USER || '25s',
    },
    article_anon_fixed_viewer: {
      executor: 'constant-vus',
      exec: 'anonymousFixedViewerScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_ANON_FIXED || 1),
      duration: __ENV.K6_DURATION_ARTICLE_ANON_FIXED || '20s',
      startTime: __ENV.K6_START_ARTICLE_ANON_FIXED || '35s',
    },
    article_anon_rotating_ip: {
      executor: 'per-vu-iterations',
      exec: 'anonymousRotatingIpScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_ANON_IP_ROTATE || 5),
      iterations: Number(__ENV.K6_ITERATIONS_ARTICLE_ANON_IP_ROTATE || 1),
      maxDuration: __ENV.K6_MAX_DURATION_ARTICLE_ANON_IP_ROTATE || '30s',
      startTime: __ENV.K6_START_ARTICLE_ANON_IP_ROTATE || '60s',
    },
    article_anon_rotating_user_agent: {
      executor: 'per-vu-iterations',
      exec: 'anonymousRotatingUserAgentScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_ANON_UA_ROTATE || 5),
      iterations: Number(__ENV.K6_ITERATIONS_ARTICLE_ANON_UA_ROTATE || 1),
      maxDuration: __ENV.K6_MAX_DURATION_ARTICLE_ANON_UA_ROTATE || '30s',
      startTime: __ENV.K6_START_ARTICLE_ANON_UA_ROTATE || '85s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    article_view_failures: ['rate==0'],
  },
};

export function setup() {
  // Given: 인증 시나리오가 1개 이상 활성화되어 있으면 로그인 정보가 준비되어 있다.
  if (!requiresAuthenticatedUsers()) {
    return {};
  }

  // When: 로그인 API를 호출해 테스트용 AccessToken 풀을 발급받는다.
  // Then: 동일 사용자/다중 사용자 시나리오가 공유한다.
  const authData = setupAuth(API_BASE_URL);
  validateMultiUserCapacity(authData.accessTokens.length);
  return authData;
}

export function sameUserRepeatScenario(data) {
  // Given: 동일 로그인 사용자가 같은 게시글을 반복 조회한다.
  const accessToken = data.accessToken || selectAccessToken(data);

  // When: 게시글 상세 조회 API를 반복 호출한다.
  const response = http.get(`${API_BASE_URL}/articles/${ARTICLE_ID}`, authParams(accessToken));

  // Then: 응답 상태와 상세 payload 구조를 검증한다.
  recordDetailResponseChecks(response, ARTICLE_ID);
  sleep(0.2);
}

export function multiUserFanOutScenario(data) {
  // Given: 서로 다른 로그인 사용자들이 같은 게시글을 동시에 조회한다.
  const accessToken = selectAccessToken(data);

  // When: 게시글 상세 조회 API를 1회 호출한다.
  const response = http.get(`${API_BASE_URL}/articles/${ARTICLE_ID}`, authParams(accessToken));

  // Then: 응답 상태와 상세 payload 구조를 검증한다.
  recordDetailResponseChecks(response, ARTICLE_ID);
}

export function anonymousFixedViewerScenario() {
  // Given: 동일 익명 식별자(IP + User-Agent)가 같은 게시글을 반복 조회한다.
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}`,
    { headers: fixedAnonymousHeaders() }
  );

  // Then: 응답 상태와 상세 payload 구조를 검증한다.
  recordDetailResponseChecks(response, ARTICLE_ID);
  sleep(0.2);
}

export function anonymousRotatingIpScenario() {
  // Given: IP만 회전하고 User-Agent는 고정된 익명 사용자들이 같은 게시글을 조회한다.
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}`,
    { headers: rotatingIpFixedUserAgentHeaders() }
  );

  // Then: 응답 상태와 상세 payload 구조를 검증한다.
  recordDetailResponseChecks(response, ARTICLE_ID);
}

export function anonymousRotatingUserAgentScenario() {
  // Given: IP는 고정되고 User-Agent만 회전하는 익명 사용자들이 같은 게시글을 조회한다.
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}`,
    { headers: fixedIpRotatingUserAgentHeaders() }
  );

  // Then: 응답 상태와 상세 payload 구조를 검증한다.
  recordDetailResponseChecks(response, ARTICLE_ID);
}

function recordDetailResponseChecks(response, articleId) {
  article_view_requests.add(1);
  const ok = check(response, {
    'article view status 200': (r) => r.status === 200,
    'article view envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article view payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.id === articleId && typeof body.data.hit === 'number';
    },
  });
  article_view_failures.add(!ok);
}

function requiresAuthenticatedUsers() {
  return (
    Number(__ENV.K6_VUS_ARTICLE_SAME_USER || 1) > 0 ||
    Number(__ENV.K6_VUS_ARTICLE_MULTI_USER || 5) > 0
  );
}

function validateMultiUserCapacity(poolSize) {
  const multiUserVus = Number(__ENV.K6_VUS_ARTICLE_MULTI_USER || 5);
  if (multiUserVus > 0 && poolSize < multiUserVus) {
    throw new Error(
      `다중 사용자 시나리오용 계정 수가 부족합니다. required=${multiUserVus}, actual=${poolSize}`
    );
  }
}
