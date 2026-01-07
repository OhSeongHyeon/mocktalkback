package com.mocktalkback.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "rt:sid:"; // rt:sid:<sid> -> jti

    private final StringRedisTemplate redis;
    private final JwtTokenProvider jwt;

    public RefreshTokenService(StringRedisTemplate redis, JwtTokenProvider jwt) {
        this.redis = redis;
        this.jwt = jwt;
    }

    public IssuedRefresh issue(Long userId) {
        String sid = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        saveSidJti(sid, jti);

        String refresh = jwt.createRefreshToken(userId, sid, jti);
        return new IssuedRefresh(refresh, sid, jti);
    }

    public Rotated rotate(String refreshToken) {
        Claims c = parseRefresh(refreshToken);

        Long userId = Long.valueOf(c.getSubject());
        String sid = (String) c.get("sid");
        String jti = c.getId();

        String key = key(sid);
        String currentJti = redis.opsForValue().get(key);

        // 세션이 없거나, jti가 다르면 = 이미 회전됐거나/탈취로 재사용 시도
        if (currentJti == null || !currentJti.equals(jti)) {
            // “의심 세션”은 바로 폐기
            redis.delete(key);
            throw new ResponseStatusException(UNAUTHORIZED, "REFRESH_INVALID");
        }

        // 정상 -> 회전
        String newJti = UUID.randomUUID().toString();
        saveSidJti(sid, newJti);

        String newRefresh = jwt.createRefreshToken(userId, sid, newJti);
        return new Rotated(userId, newRefresh);
    }

    public void revoke(String refreshToken) {
        Claims c = parseRefresh(refreshToken);
        String sid = (String) c.get("sid");
        redis.delete(key(sid));
    }

    private void saveSidJti(String sid, String jti) {
        redis.opsForValue().set(key(sid), jti, Duration.ofSeconds(jwt.refreshTtlSec()));
    }

    private String key(String sid) {
        return KEY_PREFIX + sid;
    }

    private Claims parseRefresh(String token) {
        try {
            return jwt.parseRefreshClaims(token);
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "REFRESH_EXPIRED");
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "REFRESH_INVALID");
        }
    }

    public record IssuedRefresh(String refreshToken, String sid, String jti) {}
    public record Rotated(Long userId, String refreshToken) {}
}
