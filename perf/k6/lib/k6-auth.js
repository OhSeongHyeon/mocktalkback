import http from 'k6/http';
import { check } from 'k6';

export function resolveApiBaseUrl() {
  const baseUrl = (__ENV.BASE_URL || 'http://localhost:8082').replace(/\/$/, '');
  return `${baseUrl}/api`;
}

export function requireLoginEnv() {
  const loginId = __ENV.K6_LOGIN_ID;
  const password = __ENV.K6_PASSWORD;
  if (!loginId || !password) {
    throw new Error('K6_LOGIN_ID, K6_PASSWORD 환경변수가 필요합니다.');
  }
  return { loginId, password };
}

export function loginAndGetAccessToken(apiBaseUrl, loginId, password) {
  const payload = JSON.stringify({
    loginId,
    password,
    rememberMe: false,
  });

  const response = http.post(`${apiBaseUrl}/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const ok = check(response, {
    'login status 200': (r) => r.status === 200,
  });

  if (!ok) {
    throw new Error(`로그인 실패: status=${response.status}, body=${response.body}`);
  }

  const body = response.json();
  if (!body || !body.accessToken) {
    throw new Error(`AccessToken 파싱 실패: body=${response.body}`);
  }

  return body.accessToken;
}

export function authParams(accessToken) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  };
}

export function authJsonParams(accessToken) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  };
}
