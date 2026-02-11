import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8082').replace(/\/$/, '');
const API_BASE_URL = `${BASE_URL}/api`;

const LOGIN_ID = __ENV.K6_LOGIN_ID;
const PASSWORD = __ENV.K6_PASSWORD;
const ARTICLE_ID = Number(__ENV.K6_ARTICLE_ID || 1);
const COMMENT_ID = Number(__ENV.K6_COMMENT_ID || 1);

export const consistency_failures = new Rate('consistency_failures');
export const reaction_requests = new Counter('reaction_requests');

export const options = {
  scenarios: {
    article_reaction_toggle: {
      executor: 'constant-vus',
      exec: 'articleReactionToggleScenario',
      vus: Number(__ENV.K6_VUS_ARTICLE_REACTION || 20),
      duration: __ENV.K6_DURATION_ARTICLE_REACTION || '30s',
    },
    comment_reaction_toggle: {
      executor: 'constant-vus',
      exec: 'commentReactionToggleScenario',
      vus: Number(__ENV.K6_VUS_COMMENT_REACTION || 20),
      duration: __ENV.K6_DURATION_COMMENT_REACTION || '30s',
      startTime: __ENV.K6_START_COMMENT_REACTION || '5s',
    },
    snapshot_resync: {
      executor: 'constant-vus',
      exec: 'snapshotResyncScenario',
      vus: Number(__ENV.K6_VUS_SNAPSHOT || 5),
      duration: __ENV.K6_DURATION_SNAPSHOT || '30s',
      startTime: __ENV.K6_START_SNAPSHOT || '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1200'],
    consistency_failures: ['rate==0'],
  },
};

export function setup() {
  if (!LOGIN_ID || !PASSWORD) {
    throw new Error('K6_LOGIN_ID, K6_PASSWORD 환경변수가 필요합니다.');
  }

  const loginPayload = JSON.stringify({
    loginId: LOGIN_ID,
    password: PASSWORD,
    rememberMe: false,
  });

  const response = http.post(`${API_BASE_URL}/auth/login`, loginPayload, {
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

  return {
    accessToken: body.accessToken,
  };
}

export function articleReactionToggleScenario(data) {
  const reactionType = Math.random() < 0.5 ? 1 : -1;
  const response = http.post(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(data.accessToken)
  );

  reaction_requests.add(1);
  const ok = check(response, {
    'article reaction status 200': (r) => r.status === 200,
    'article reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'article reaction summary valid': (r) => {
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
  consistency_failures.add(!ok);
  sleep(0.15);
}

export function commentReactionToggleScenario(data) {
  const reactionType = Math.random() < 0.5 ? 1 : -1;
  const response = http.post(
    `${API_BASE_URL}/comments/${COMMENT_ID}/reactions`,
    JSON.stringify({ reactionType }),
    authJsonParams(data.accessToken)
  );

  reaction_requests.add(1);
  const ok = check(response, {
    'comment reaction status 200': (r) => r.status === 200,
    'comment reaction envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'comment reaction summary valid': (r) => {
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
  consistency_failures.add(!ok);
  sleep(0.15);
}

export function snapshotResyncScenario(data) {
  const response = http.get(
    `${API_BASE_URL}/articles/${ARTICLE_ID}/comments/snapshot?page=0&size=20`,
    authParams(data.accessToken)
  );

  const ok = check(response, {
    'snapshot status 200': (r) => r.status === 200,
    'snapshot envelope success': (r) => {
      const body = r.json();
      return body && body.success === true;
    },
    'snapshot has syncVersion': (r) => {
      const body = r.json();
      return body && body.data && typeof body.data.syncVersion === 'number';
    },
  });
  consistency_failures.add(!ok);
  sleep(0.3);
}

function authParams(accessToken) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  };
}

function authJsonParams(accessToken) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  };
}
