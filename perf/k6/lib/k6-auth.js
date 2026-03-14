import http from 'k6/http';
import { check } from 'k6';

export function resolveApiBaseUrl() {
  const baseUrl = (__ENV.BASE_URL || 'http://localhost:8082').replace(/\/$/, '');
  return `${baseUrl}/api`;
}

export function requireLoginEnv() {
  const credentials = resolveLoginCredentials();
  return credentials[0];
}

export function resolveLoginCredentials() {
  const pool = parseCredentialPool(__ENV.K6_LOGIN_USERS);
  if (pool.length > 0) {
    return pool;
  }
  const loginId = __ENV.K6_LOGIN_ID;
  const password = __ENV.K6_PASSWORD;
  if (!loginId || !password) {
    throw new Error('K6_LOGIN_ID, K6_PASSWORD 또는 K6_LOGIN_USERS 환경변수가 필요합니다.');
  }
  return [{ loginId, password }];
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
        throw new Error(`K6_LOGIN_USERS 형식이 올바르지 않습니다: ${entry}`);
      }
      const loginId = entry.slice(0, separatorIndex).trim();
      const password = entry.slice(separatorIndex + 1).trim();
      if (!loginId || !password) {
        throw new Error(`K6_LOGIN_USERS 형식이 올바르지 않습니다: ${entry}`);
      }
      return { loginId, password };
    });
}

export function createAccessTokenPool(apiBaseUrl) {
  const credentials = resolveLoginCredentials();
  const accessTokens = credentials.map((credential) =>
    loginAndGetAccessToken(apiBaseUrl, credential.loginId, credential.password)
  );
  return { credentials, accessTokens };
}

export function setupAuth(apiBaseUrl) {
  const { credentials, accessTokens } = createAccessTokenPool(apiBaseUrl);
  return {
    credentials,
    accessTokens,
    accessToken: accessTokens[0],
  };
}

export function selectAccessToken(data) {
  if (Array.isArray(data?.accessTokens) && data.accessTokens.length > 0) {
    const vuNumber = Number(__VU || 1);
    const tokenIndex = Math.max(0, (vuNumber - 1) % data.accessTokens.length);
    return data.accessTokens[tokenIndex];
  }
  if (data?.accessToken) {
    return data.accessToken;
  }
  throw new Error('사용 가능한 AccessToken이 없습니다.');
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

export function authParams(accessToken, extraHeaders = {}) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...extraHeaders,
    },
  };
}

export function authJsonParams(accessToken, extraHeaders = {}) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      ...extraHeaders,
    },
  };
}
