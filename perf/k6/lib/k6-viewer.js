function resolveInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

const FIXED_IP = __ENV.K6_ANON_IP_FIXED || '198.51.100.10';
const FIXED_USER_AGENT = __ENV.K6_ANON_USER_AGENT_FIXED || 'k6-anon-fixed/1.0';
const IP_PREFIX = __ENV.K6_ANON_IP_PREFIX || '198.51.100';
const USER_AGENT_PREFIX = __ENV.K6_ANON_USER_AGENT_PREFIX || 'k6-anon-rotating';

export function anonymousHeaders(clientIp, userAgent) {
  return {
    'X-Forwarded-For': clientIp,
    'X-Real-IP': clientIp,
    'User-Agent': userAgent,
  };
}

export function fixedAnonymousHeaders() {
  return anonymousHeaders(FIXED_IP, FIXED_USER_AGENT);
}

export function rotatingIpFixedUserAgentHeaders() {
  return anonymousHeaders(resolveRotatingIp(), FIXED_USER_AGENT);
}

export function fixedIpRotatingUserAgentHeaders() {
  return anonymousHeaders(FIXED_IP, resolveRotatingUserAgent());
}

export function rotatingAnonymousHeaders() {
  return anonymousHeaders(resolveRotatingIp(), resolveRotatingUserAgent());
}

function resolveRotatingIp() {
  const vuNumber = resolveInteger(__VU, 1);
  const iteration = resolveInteger(__ITER, 0);
  const lastOctet = 11 + ((vuNumber * 17 + iteration) % 200);
  return `${IP_PREFIX}.${lastOctet}`;
}

function resolveRotatingUserAgent() {
  const vuNumber = resolveInteger(__VU, 1);
  const iteration = resolveInteger(__ITER, 0);
  return `${USER_AGENT_PREFIX}/vu-${vuNumber}-iter-${iteration}`;
}
