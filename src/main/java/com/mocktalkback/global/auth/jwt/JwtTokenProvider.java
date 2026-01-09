package com.mocktalkback.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final String issuer;
    private final long accessTtlSec;
    private final long refreshTtlSec;
    private final long refreshAbsoluteTtlSec;

    public JwtTokenProvider(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_ISSUER:mocktalk}") String issuer,
            @Value("${JWT_ACCESS_TTL_SECONDS:3600}") long accessTtlSec,
            @Value("${JWT_REFRESH_TTL_SECONDS:1209600}") long refreshTtlSec,  // 14days default
            @Value("${JWT_REFRESH_ABSOLUTE_TTL_SECONDS:2592000}") long refreshAbsoluteTtlSec  // 30days default
    ) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes for HS256.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTtlSec = accessTtlSec;
        this.refreshTtlSec = refreshTtlSec;
        this.refreshAbsoluteTtlSec = refreshAbsoluteTtlSec;
    }

    public long accessTtlSec() {
        return accessTtlSec;
    }

    public long refreshTtlSec() {
        return refreshTtlSec;
    }

    public long refreshAbsoluteTtlSec() {
        return refreshAbsoluteTtlSec;
    }

    public String createAccessToken(Long userId, String roleName, int authBit) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSec);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("typ", "access")
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
                .requireIssuer(issuer)
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
                .id(jti)  // jti
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseRefreshClaims(String token) throws JwtException {
        Claims c = parseClaims(token);  // 기존 parseClaims 재사용
        Object typ = c.get("typ");
        if (!"refresh".equals(typ)) {
            throw new IllegalArgumentException("not refresh token");
        }
        return c;
    }


}
