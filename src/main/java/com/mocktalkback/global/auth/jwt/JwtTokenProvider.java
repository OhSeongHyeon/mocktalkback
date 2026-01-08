package com.mocktalkback.global.auth.jwt;

import io.jsonwebtoken.Claims;
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

    public JwtTokenProvider(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_ISSUER:mocktalk}") String issuer,
            @Value("${JWT_ACCESS_TTL_SECONDS:3600}") long accessTtlSec
    ) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes for HS256.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTtlSec = accessTtlSec;
    }

    public long accessTtlSec() {
        return accessTtlSec;
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
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
