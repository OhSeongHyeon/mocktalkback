-- Atomic refresh rotation with absolute max TTL enforcement.
-- KEYS[1] = rt:sid:<sid> (current jti)
-- KEYS[2] = rt:sid:abs:<sid> (absolute expiry epoch sec)
-- ARGV[1] = current jti
-- ARGV[2] = new jti
-- ARGV[3] = now epoch sec
-- ARGV[4] = refresh ttl sec

local current = redis.call("GET", KEYS[1])
local abs = redis.call("GET", KEYS[2])
if not abs then
  if current then
    redis.call("DEL", KEYS[1])
  end
  return 0
end

local now = tonumber(ARGV[3])
local absExp = tonumber(abs)
if not absExp then
  redis.call("DEL", KEYS[1])
  redis.call("DEL", KEYS[2])
  return 0
end

local remaining = absExp - now
if remaining <= 0 then
  redis.call("DEL", KEYS[1])
  redis.call("DEL", KEYS[2])
  return -1
end

if (not current) or current ~= ARGV[1] then
  redis.call("DEL", KEYS[1])
  redis.call("DEL", KEYS[2])
  return 0
end

local refreshTtl = tonumber(ARGV[4])
local newTtl = refreshTtl
if remaining < refreshTtl then
  newTtl = remaining
end

redis.call("SET", KEYS[1], ARGV[2], "EX", newTtl)
redis.call("EXPIRE", KEYS[2], remaining)
return newTtl
