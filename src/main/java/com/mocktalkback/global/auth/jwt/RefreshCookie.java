package com.mocktalkback.global.auth.jwt;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookie {

    // 프론트/배포 환경에서 true로 (https)
    private final boolean secure = false;
    
    public static final String COOKIE_NAME = "refresh_token";
    
    public ResponseCookie create(String refreshToken, long maxAgeSec) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/auth")         // /auth/refresh, /auth/logout에만 전송
                .maxAge(maxAgeSec)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(0)
                .build();
    }
}
