package com.mocktalkback.global.auth.oauth2;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OAuth2CodeService {

    private static final String KEY_PREFIX = "oauth:code:";

    private final StringRedisTemplate redis;
    private final long ttlSec;
    private final RedisScript<String> consumeScript;

    public OAuth2CodeService(
            StringRedisTemplate redis,
            OAuth2Properties oAuth2Properties
    ) {
        this.redis = redis;
        this.ttlSec = oAuth2Properties.getCodeTtlSeconds();
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText("local v=redis.call('get',KEYS[1]); if v then redis.call('del',KEYS[1]); end; return v");
        this.consumeScript = script;
    }

    public String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (ttlSec <= 0) {
            throw new IllegalStateException("code ttl must be positive");
        }
        String code = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(key(code), String.valueOf(userId), Duration.ofSeconds(ttlSec));
        return code;
    }

    public Long consume(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String raw = redis.execute(consumeScript, List.of(key(code)));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return Long.valueOf(raw);
    }

    private String key(String code) {
        return KEY_PREFIX + code;
    }
}
