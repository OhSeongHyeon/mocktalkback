import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
  authJsonParams,
  authParams,
  loginAndGetAccessToken,
  requireLoginEnv,
  resolveApiBaseUrl,
} from './lib/k6-auth.js';

const API_BASE_URL = resolveApiBaseUrl();
const ARTICLE_ID = Number(__ENV.K6_ARTICLE_ID || 1);

export const article_failures = new Rate('article_failures');
export const article_requests = new Counter('article_requests');

export const options = {
  scenarios: {
    article_detail_read: {
      executor: 'constant-vus',
      exec: 'articleDetailReadScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_READ || 5),
      duration: __ENV.K6_DURATION_ARTICLE_READ || '20s',
    },
    article_reaction_toggle: {
      executor: 'constant-vus',
      exec: 'articleReactionToggleScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_REACTION || 5),
      duration: __ENV.K6_DURATION_ARTICLE_REACTION || '20s',
      startTime: __ENV.K6_START_ARTICLE_REACTION || '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    article_failures: ['rate==0'],
  },
};

export function setup() {
  // Given: 테스트 사용자 로그인 정보를 환경변수로 받는다.
  const { loginId, password } = requireLoginEnv();
  // When: 로그인 API를 호출해 AccessToken을 발급받는다.
  const accessToken = loginAndGetAccessToken(API_BASE_URL, loginId, password);
  // Then: 이후 시나리오에서 재사용할 인증 토큰을 반환한다.
  return { accessToken };
}

export function articleDetailReadScenario(data) {
  // Given: 대상 게시글 ID와 인증 토큰이 준비되어 있다.
  // When: 게시글 상세 조회 API를 반복 호출한다.
  const response = http.get(`${API_BASE_URL}/articles/${ARTICLE_ID}`, authParams(data.accessToken));
  article_requests.add(1);

  // Then: 응답 상태/엔벨로프/핵심 필드 형식을 검증한다.
  const ok = check(response, {
    'article detail status 200': (r) => r.status === 200,
    'article detail envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article detail payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.id === ARTICLE_ID;
    },
  });

  article_failures.add(!ok);
  sleep(0.2);
}

export function articleReactionToggleScenario(data) {
  // Given: 대상 게시글 반응 토글 요청값(-1 또는 1)을 준비한다.
  const reactionType = Math.random() < 0.5 ? 1 : -1;

  // When: 게시글 반응 토글 API를 호출한다.
  const response = http.post(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(data.accessToken)
  );
  article_requests.add(1);

  // Then: 반응 요약 응답의 정합성을 검증한다.
  const ok = check(response, {
    'article reaction status 200': (r) => r.status === 200,
    'article reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article reaction payload valid': (r) => {
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

  article_failures.add(!ok);
  sleep(0.2);
}
