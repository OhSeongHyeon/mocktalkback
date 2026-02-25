package com.mocktalkback.global.auth.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "rt:sid:";  // rt:sid:<sid> -> jti
    private static final String ABS_KEY_PREFIX = "rt:sid:abs:";  // rt:sid:abs:<sid> -> abs exp epoch sec
    private static final String ROTATE_MODE_ATOMIC = "atomic";
    private static final String ROTATE_MODE_NON_ATOMIC = "non-atomic";

    private final StringRedisTemplate redis;
    private final JwtTokenProvider jwt;
    private final RedisScript<Long> rotateScript;
    private final String rotateMode;
    private final long reproDelayMs;

    public RefreshTokenService(
            StringRedisTemplate redis,
            JwtTokenProvider jwt,
            @Value("${AUTH_REFRESH_ROTATE_MODE:atomic}") String rotateMode,
            @Value("${AUTH_REFRESH_REPRO_DELAY_MS:0}") long reproDelayMs
    ) {
        this.redis = redis;
        this.jwt = jwt;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/refresh_rotate.lua"));
        script.setResultType(Long.class);
        this.rotateScript = script;
        this.rotateMode = normalizeRotateMode(rotateMode);
        this.reproDelayMs = Math.max(0L, reproDelayMs);
        log.info(
                "refresh rotate mode configured mode={}, reproDelayMs={} (재현 모드는 테스트 브랜치 전용)",
                this.rotateMode,
                this.reproDelayMs
        );
    }

    public IssuedRefresh issue(Long userId, boolean rememberMe) {
        String sid = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        long nowSec = Instant.now().getEpochSecond();
        long absExpEpochSec = nowSec + jwt.refreshAbsoluteTtlSec();
        long absTtlSec = absExpEpochSec - nowSec;
        if (absTtlSec <= 0) {
            throw new IllegalStateException("refresh absolute ttl must be positive");
        }
        long jtiTtlSec = Math.min(jwt.refreshTtlSec(), absTtlSec);

        saveAbsExp(sid, absExpEpochSec, absTtlSec);
        saveSidJti(sid, jti, jtiTtlSec);

        String refresh = jwt.createRefreshToken(userId, sid, jti, rememberMe);
        return new IssuedRefresh(refresh, sid, jti, jtiTtlSec);
    }

    public Rotated rotate(String refreshToken) {
        Claims c = parseRefresh(refreshToken);

        Long userId = Long.valueOf(c.getSubject());
        String sid = (String) c.get("sid");
        String jti = c.getId();
        boolean rememberMe = resolveRememberMe(c);

        if (sid == null || jti == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID");
        }

        if (ROTATE_MODE_NON_ATOMIC.equals(rotateMode)) {
            return rotateWithoutAtomicForRepro(userId, sid, jti, rememberMe);
        }

        return rotateAtomically(userId, sid, jti, rememberMe);
    }

    private Rotated rotateAtomically(Long userId, String sid, String jti, boolean rememberMe) {
        String newJti = UUID.randomUUID().toString();
        long nowSec = Instant.now().getEpochSecond();

        // Lua 스크립트로 비교/회전을 원자적으로 처리합니다.
        // Lua: compare current jti and rotate atomically while honoring absolute max TTL.
        Long result = redis.execute(
                rotateScript,
                List.of(key(sid), absKey(sid)),
                jti,
                newJti,
                String.valueOf(nowSec),
                String.valueOf(jwt.refreshTtlSec())
        );

        if (result == null || result == 0L) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID");
        }
        if (result < 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRED");
        }

        String newRefresh = jwt.createRefreshToken(userId, sid, newJti, rememberMe);
        return new Rotated(userId, newRefresh, result, rememberMe);
    }

    private Rotated rotateWithoutAtomicForRepro(Long userId, String sid, String jti, boolean rememberMe) {
        String current = redis.opsForValue().get(key(sid));
        String abs = redis.opsForValue().get(absKey(sid));

        if (abs == null) {
            if (current != null) {
                redis.delete(key(sid));
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID");
        }

        long nowSec = Instant.now().getEpochSecond();
        long absExpEpochSec;
        try {
            absExpEpochSec = Long.parseLong(abs);
        } catch (NumberFormatException e) {
            redis.delete(List.of(key(sid), absKey(sid)));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID");
        }

        long remaining = absExpEpochSec - nowSec;
        if (remaining <= 0) {
            redis.delete(List.of(key(sid), absKey(sid)));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRED");
        }

        if (current == null || !current.equals(jti)) {
            redis.delete(List.of(key(sid), absKey(sid)));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID");
        }

        delayForRepro();

        String newJti = UUID.randomUUID().toString();
        long newTtl = Math.min(jwt.refreshTtlSec(), remaining);
        saveSidJti(sid, newJti, newTtl);
        redis.expire(absKey(sid), Duration.ofSeconds(remaining));

        String newRefresh = jwt.createRefreshToken(userId, sid, newJti, rememberMe);
        return new Rotated(userId, newRefresh, newTtl, rememberMe);
    }

    private void delayForRepro() {
        if (reproDelayMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(reproDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("refresh rotate 재현 지연 중 인터럽트가 발생했습니다.", e);
        }
    }

    private String normalizeRotateMode(String rawMode) {
        if (rawMode == null) {
            return ROTATE_MODE_ATOMIC;
        }
        String normalized = rawMode.trim().toLowerCase(Locale.ROOT);
        if (ROTATE_MODE_ATOMIC.equals(normalized) || ROTATE_MODE_NON_ATOMIC.equals(normalized)) {
            return normalized;
        }
        log.warn("unknown AUTH_REFRESH_ROTATE_MODE={} -> atomic으로 강제 적용", rawMode);
        return ROTATE_MODE_ATOMIC;
    }

    public void revoke(String refreshToken) {
        Claims c = parseRefresh(refreshToken);
        String sid = (String) c.get("sid");
        redis.delete(List.of(key(sid), absKey(sid)));
    }

    // private void saveSidJti(String sid, String jti) {
    //     saveSidJti(sid, jti, jwt.refreshTtlSec());
    // }

    private void saveSidJti(String sid, String jti, long ttlSec) {
        redis.opsForValue().set(key(sid), jti, Duration.ofSeconds(ttlSec));
    }

    private void saveAbsExp(String sid, long absExpEpochSec, long ttlSec) {
        redis.opsForValue().set(absKey(sid), String.valueOf(absExpEpochSec), Duration.ofSeconds(ttlSec));
    }

    private String key(String sid) {
        return KEY_PREFIX + sid;
    }

    private String absKey(String sid) {
        return ABS_KEY_PREFIX + sid;
    }

    private Claims parseRefresh(String token) {
        try {
            return jwt.parseRefreshClaims(token);
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRED");
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID");
        }
    }

    private boolean resolveRememberMe(Claims c) {
        Object raw = c.get("rm");
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String value) {
            return Boolean.parseBoolean(value);
        }
        if (raw instanceof Number value) {
            return value.intValue() != 0;
        }
        return true;
    }

    public record IssuedRefresh(
            String refreshToken,
            String sid,
            String jti,
            long refreshExpiresInSec
    ) {}

    public record Rotated(
            Long userId,
            String refreshToken,
            long refreshExpiresInSec,
            boolean rememberMe
    ) {}


}
