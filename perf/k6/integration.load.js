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
const COMMENT_ID = Number(__ENV.K6_COMMENT_ID || 1);
const SEARCH_QUERY = __ENV.K6_SEARCH_QUERY || '공지';

export const integration_failures = new Rate('integration_failures');
export const integration_requests = new Counter('integration_requests');

export const options = {
  scenarios: {
    article_reaction_toggle: {
      executor: 'constant-vus',
      exec: 'articleReactionToggleScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_REACTION || 5),
      duration: __ENV.K6_DURATION_ARTICLE_REACTION || '20s',
    },
    comment_reaction_toggle: {
      executor: 'constant-vus',
      exec: 'commentReactionToggleScenario',
      vus: Number(__ENV.K6_VUS_COMMENT_REACTION || 5),
      duration: __ENV.K6_DURATION_COMMENT_REACTION || '20s',
      startTime: __ENV.K6_START_COMMENT_REACTION || '5s',
    },
    snapshot_resync: {
      executor: 'constant-vus',
      exec: 'snapshotResyncScenario',
      vus: Number(__ENV.K6_VUS_SNAPSHOT || 3),
      duration: __ENV.K6_DURATION_SNAPSHOT || '20s',
      startTime: __ENV.K6_START_SNAPSHOT || '10s',
    },
    search_all: {
      executor: 'constant-vus',
      exec: 'searchAllScenario',
      vus: Number(__ENV.K6_VUS_SEARCH_ALL || 3),
      duration: __ENV.K6_DURATION_SEARCH_ALL || '20s',
      startTime: __ENV.K6_START_SEARCH_ALL || '12s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    integration_failures: ['rate==0'],
  },
};

export function setup() {
  // Given: 통합 시나리오에 필요한 로그인 환경변수가 준비되어 있다.
  const { loginId, password } = requireLoginEnv();
  // When: 로그인 API로 AccessToken을 발급받는다.
  const accessToken = loginAndGetAccessToken(API_BASE_URL, loginId, password);
  // Then: 모든 도메인 시나리오가 공통 인증 토큰을 사용한다.
  return { accessToken };
}

export function articleReactionToggleScenario(data) {
  // Given: 게시글 반응 토글 요청값(-1 또는 1)을 준비한다.
  const reactionType = Math.random() < 0.5 ? 1 : -1;
  // When: 게시글 반응 토글 API를 호출한다.
  const response = http.post(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(data.accessToken)
  );
  integration_requests.add(1);

  // Then: 응답 코드/엔벨로프/반응 요약 정합성을 검증한다.
  const ok = check(response, {
    'integration article reaction status 200': (r) => r.status === 200,
    'integration article reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'integration article reaction payload valid': (r) => {
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
  integration_failures.add(!ok);
  sleep(0.15);
}

export function commentReactionToggleScenario(data) {
  // Given: 댓글 반응 토글 요청값(-1 또는 1)을 준비한다.
  const reactionType = Math.random() < 0.5 ? 1 : -1;
  // When: 댓글 반응 토글 API를 호출한다.
  const response = http.post(
    `${API_BASE_URL}/comments/${COMMENT_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(data.accessToken)
  );
  integration_requests.add(1);

  // Then: 응답 코드/엔벨로프/반응 요약 정합성을 검증한다.
  const ok = check(response, {
    'integration comment reaction status 200': (r) => r.status === 200,
    'integration comment reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'integration comment reaction payload valid': (r) => {
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
  integration_failures.add(!ok);
  sleep(0.15);
}

export function snapshotResyncScenario(data) {
  // Given: 댓글 동기화 스냅샷 조회 대상 게시글이 준비되어 있다.
  // When: 댓글 스냅샷 API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/comments/snapshot?page=0&size=20`,
    authParams(data.accessToken)
  );
  integration_requests.add(1);

  // Then: syncVersion과 페이지 구조를 검증한다.
  const ok = check(response, {
    'integration snapshot status 200': (r) => r.status === 200,
    'integration snapshot envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'integration snapshot payload valid': (r) => {
      const body = r.json();
      return body && body.data && typeof body.data.syncVersion === 'number' && body.data.page;
    },
  });
  integration_failures.add(!ok);
  sleep(0.25);
}

export function searchAllScenario(data) {
  // Given: 검색어와 인증 토큰이 준비되어 있다.
  // When: 통합 검색(ALL) API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/search?q=${encodeURIComponent(SEARCH_QUERY)}&type=ALL&page=0&size=10`,
    authParams(data.accessToken)
  );
  integration_requests.add(1);

  // Then: 통합 검색 응답 구조를 검증한다.
  const ok = check(response, {
    'integration search status 200': (r) => r.status === 200,
    'integration search envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'integration search payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.articles && body.data.comments;
    },
  });
  integration_failures.add(!ok);
  sleep(0.25);
}
