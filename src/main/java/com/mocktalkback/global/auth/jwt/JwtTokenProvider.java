package com.mocktalkback.global.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final String issuer;
    private final long accessTtlSec;
    private final long refreshTtlSec;

    public JwtTokenProvider(
        @Value("${JWT_SECRET}") String secret,
        @Value("${JWT_ISSUER:mocktalk}") String issuer,
        @Value("${JWT_ACCESS_TTL_SEC:3600}") long accessTtlSec,
        @Value("${JWT_REFRESH_TTL_SEC:1209600}") long refreshTtlSec // 14d default
    ) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes for HS256.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTtlSec = accessTtlSec;
        this.refreshTtlSec = refreshTtlSec;
    }

    public long accessTtlSec() {
        return accessTtlSec;
    }
    
    public long refreshTtlSec() {
        return refreshTtlSec;
    }

    public String createAccessToken(Long userId, String email, String roleName, int authBit) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSec);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))   // sub = userId
                .claim("email", email)
                .claim("role", roleName)
                .claim("authBit", authBit)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String createRefreshToken(Long userId, String sid, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtlSec);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("typ", "refresh")
                .claim("sid", sid)
                .id(jti) // jti
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseRefreshClaims(String token) throws JwtException {
        Claims c = parseClaims(token); // 기존 parseClaims 재사용
        Object typ = c.get("typ");
        if (!"refresh".equals(typ)) {
            throw new IllegalArgumentException("not refresh token");
        }
        return c;
    }


}
