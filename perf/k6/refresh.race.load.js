import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { requireLoginEnv, resolveApiBaseUrl } from './lib/k6-auth.js';

const API_BASE_URL = resolveApiBaseUrl();
const ORIGIN = __ENV.K6_ORIGIN || 'http://localhost:5173';
const REFRESH_BURST = Number(__ENV.K6_REFRESH_BURST || 30);

export const refresh_requests = new Counter('refresh_requests');
export const refresh_status_200 = new Counter('refresh_status_200');
export const refresh_status_401 = new Counter('refresh_status_401');
export const refresh_status_403 = new Counter('refresh_status_403');
export const refresh_status_other = new Counter('refresh_status_other');
export const refresh_unexpected_status = new Rate('refresh_unexpected_status');

export const options = {
  scenarios: {
    refresh_race_burst: {
      executor: 'per-vu-iterations',
      exec: 'refreshRaceScenario',
      vus: REFRESH_BURST,
      iterations: 1,
      maxDuration: __ENV.K6_DURATION_REFRESH_BURST || '30s',
    },
  },
  thresholds: {
    refresh_unexpected_status: ['rate==0'],
  },
};

export function setup() {
  // Given: 로그인 계정 정보와 Origin 헤더 값이 준비되어 있다.
  const { loginId, password } = requireLoginEnv();
  const payload = JSON.stringify({
    loginId,
    password,
    rememberMe: true,
  });

  // When: 로그인 API를 호출해 refresh_token 쿠키를 발급받는다.
  const response = http.post(`${API_BASE_URL}/auth/login`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Origin: ORIGIN,
    },
  });

  // Then: 200 응답과 refresh_token 쿠키 발급 여부를 검증한다.
  const ok = check(response, {
    'refresh race login status 200': (r) => r.status === 200,
  });
  if (!ok) {
    throw new Error(`로그인 실패: status=${response.status}, body=${response.body}`);
  }

  const refreshToken = extractRefreshToken(response);
  if (!refreshToken) {
    const setCookieHeaders = response.headers['Set-Cookie'];
    throw new Error(`refresh_token 쿠키 파싱 실패: Set-Cookie=${JSON.stringify(setCookieHeaders)}`);
  }

  return { refreshToken };
}

export function refreshRaceScenario(data) {
  // Given: setup에서 받은 동일 refresh_token을 모든 VU가 공유한다.
  const params = {
    headers: {
      Origin: ORIGIN,
      Cookie: `refresh_token=${data.refreshToken}`,
    },
  };

  // When: refresh API를 각 VU가 동시에 1회 호출한다.
  const response = http.post(`${API_BASE_URL}/auth/refresh`, null, params);
  refresh_requests.add(1);

  // Then: 상태코드를 집계해 race condition 징후를 확인한다.
  const status = response.status;
  if (status === 200) {
    refresh_status_200.add(1);
  } else if (status === 401) {
    refresh_status_401.add(1);
  } else if (status === 403) {
    refresh_status_403.add(1);
  } else {
    refresh_status_other.add(1);
  }

  const unexpected = !(status === 200 || status === 401);
  refresh_unexpected_status.add(unexpected);
}

function extractRefreshTokenFromSetCookie(setCookieHeaders) {
  if (!setCookieHeaders) {
    return null;
  }

  const headers = Array.isArray(setCookieHeaders) ? setCookieHeaders : [setCookieHeaders];
  for (const header of headers) {
    const match = String(header).match(/refresh_token=([^;]+)/);
    if (match && match[1]) {
      return match[1];
    }
  }
  return null;
}

function extractRefreshToken(response) {
  const cookieCandidates = response.cookies && response.cookies.refresh_token;
  if (Array.isArray(cookieCandidates) && cookieCandidates.length > 0) {
    const candidate = cookieCandidates[0];
    if (candidate && candidate.value) {
      return candidate.value;
    }
  }

  return extractRefreshTokenFromSetCookie(response.headers['Set-Cookie']);
}
