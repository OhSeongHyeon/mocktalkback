import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
  authParams,
  loginAndGetAccessToken,
  requireLoginEnv,
  resolveApiBaseUrl,
} from './lib/k6-auth.js';

const API_BASE_URL = resolveApiBaseUrl();
const SEARCH_QUERY = __ENV.K6_SEARCH_QUERY || '공지';
const SEARCH_PAGE_SIZE = Number(__ENV.K6_SEARCH_SIZE || 10);

export const search_failures = new Rate('search_failures');
export const search_requests = new Counter('search_requests');

export const options = {
  scenarios: {
    search_all: {
      executor: 'constant-vus',
      exec: 'searchAllScenario',
      vus: Number(__ENV.K6_VUS_SEARCH_ALL || 5),
      duration: __ENV.K6_DURATION_SEARCH_ALL || '20s',
    },
    search_article: {
      executor: 'constant-vus',
      exec: 'searchArticleScenario',
      vus: Number(__ENV.K6_VUS_SEARCH_ARTICLE || 5),
      duration: __ENV.K6_DURATION_SEARCH_ARTICLE || '20s',
      startTime: __ENV.K6_START_SEARCH_ARTICLE || '5s',
    },
    search_comment: {
      executor: 'constant-vus',
      exec: 'searchCommentScenario',
      vus: Number(__ENV.K6_VUS_SEARCH_COMMENT || 5),
      duration: __ENV.K6_DURATION_SEARCH_COMMENT || '20s',
      startTime: __ENV.K6_START_SEARCH_COMMENT || '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    search_failures: ['rate==0'],
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

export function searchAllScenario(data) {
  // Given: 검색어/페이지 파라미터와 인증 토큰이 준비되어 있다.
  // When: 통합 검색(ALL) API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/search?q=${encodeURIComponent(SEARCH_QUERY)}&type=ALL&page=0&size=${SEARCH_PAGE_SIZE}`,
    authParams(data.accessToken)
  );
  search_requests.add(1);

  // Then: 엔벨로프와 통합 검색 응답 구조를 검증한다.
  const ok = check(response, {
    'search all status 200': (r) => r.status === 200,
    'search all envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'search all payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.articles && body.data.comments;
    },
  });

  search_failures.add(!ok);
  sleep(0.2);
}

export function searchArticleScenario(data) {
  // Given: 게시글 타입 검색 파라미터가 준비되어 있다.
  // When: 게시글 검색 API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/search?q=${encodeURIComponent(SEARCH_QUERY)}&type=ARTICLE&page=0&size=${SEARCH_PAGE_SIZE}`,
    authParams(data.accessToken)
  );
  search_requests.add(1);

  // Then: 게시글 검색 결과 슬라이스 구조를 검증한다.
  const ok = check(response, {
    'search article status 200': (r) => r.status === 200,
    'search article envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'search article payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.articles && Array.isArray(body.data.articles.items);
    },
  });

  search_failures.add(!ok);
  sleep(0.2);
}

export function searchCommentScenario(data) {
  // Given: 댓글 타입 검색 파라미터가 준비되어 있다.
  // When: 댓글 검색 API를 호출한다.
  const response = http.get(
    `${API_BASE_URL}/search?q=${encodeURIComponent(SEARCH_QUERY)}&type=COMMENT&page=0&size=${SEARCH_PAGE_SIZE}`,
    authParams(data.accessToken)
  );
  search_requests.add(1);

  // Then: 댓글 검색 결과 슬라이스 구조를 검증한다.
  const ok = check(response, {
    'search comment status 200': (r) => r.status === 200,
    'search comment envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'search comment payload valid': (r) => {
      const body = r.json();
      return body && body.data && body.data.comments && Array.isArray(body.data.comments.items);
    },
  });

  search_failures.add(!ok);
  sleep(0.2);
}
