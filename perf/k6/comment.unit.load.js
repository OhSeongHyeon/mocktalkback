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

export const comment_failures = new Rate('comment_failures');
export const comment_requests = new Counter('comment_requests');

export const options = {
  scenarios: {
    comment_list_read: {
      executor: 'constant-vus',
      exec: 'commentListReadScenario',
      vus: Number(__ENV.K6_VUS_COMMENT_LIST || 5),
      duration: __ENV.K6_DURATION_COMMENT_LIST || '20s',
    },
    comment_snapshot_read: {
      executor: 'constant-vus',
      exec: 'commentSnapshotReadScenario',
      vus: Number(__ENV.K6_VUS_COMMENT_SNAPSHOT || 5),
      duration: __ENV.K6_DURATION_COMMENT_SNAPSHOT || '20s',
      startTime: __ENV.K6_START_COMMENT_SNAPSHOT || '5s',
    },
    comment_reaction_toggle: {
      executor: 'constant-vus',
      exec: 'commentReactionToggleScenario',
      vus: Number(__ENV.K6_VUS_COMMENT_REACTION || 5),
      duration: __ENV.K6_DURATION_COMMENT_REACTION || '20s',
      startTime: __ENV.K6_START_COMMENT_REACTION || '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    comment_failures: ['rate==0'],
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

export function commentListReadScenario(data) {
  // Given: 대상 게시글 ID와 인증 토큰이 준비되어 있다.
  // When: 댓글 트리 조회 API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/comments?page=0&size=20`,
    authParams(data.accessToken)
  );
  comment_requests.add(1);

  // Then: 엔벨로프/페이지 구조를 검증한다.
  const ok = check(response, {
    'comment list status 200': (r) => r.status === 200,
    'comment list envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'comment list payload valid': (r) => {
      const body = r.json();
      return body && body.data && Array.isArray(body.data.items);
    },
  });

  comment_failures.add(!ok);
  sleep(0.2);
}

export function commentSnapshotReadScenario(data) {
  // Given: 대상 게시글 ID와 인증 토큰이 준비되어 있다.
  // When: 댓글 스냅샷 API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/comments/snapshot?page=0&size=20`,
    authParams(data.accessToken)
  );
  comment_requests.add(1);

  // Then: syncVersion과 page 구조를 검증한다.
  const ok = check(response, {
    'comment snapshot status 200': (r) => r.status === 200,
    'comment snapshot envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'comment snapshot payload valid': (r) => {
      const body = r.json();
      return body && body.data && typeof body.data.syncVersion === 'number' && body.data.page;
    },
  });

  comment_failures.add(!ok);
  sleep(0.2);
}

export function commentReactionToggleScenario(data) {
  // Given: 대상 댓글 반응 토글 요청값(-1 또는 1)을 준비한다.
  const reactionType = Math.random() < 0.5 ? 1 : -1;

  // When: 댓글 반응 토글 API를 호출한다.
  const response = http.post(
    `${API_BASE_URL}/comments/${COMMENT_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(data.accessToken)
  );
  comment_requests.add(1);

  // Then: 반응 요약 응답의 정합성을 검증한다.
  const ok = check(response, {
    'comment reaction status 200': (r) => r.status === 200,
    'comment reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'comment reaction payload valid': (r) => {
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

  comment_failures.add(!ok);
  sleep(0.2);
}
