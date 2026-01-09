package com.mocktalkback.global.auth;  // CookieUtil, RefreshCookie

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final boolean secure;

    public CookieUtil(@Value("${SECURITY_COOKIE_SECURE:false}") boolean secure) {
        this.secure = secure;
    }

    public static final String COOKIE_NAME = "refresh_token";

    public ResponseCookie create(String refreshToken, long maxAgeSec) {

        final String samSite = "Lax";
        final String path = "/api/auth";

        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(samSite)
                .path(path)
                .maxAge(maxAgeSec)
                .build();
    }

    public ResponseCookie clear() {

        final String samSite = "Lax";
        final String path = "/api/auth";

        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(samSite)
                .path(path)
                .maxAge(0)
                .build();
    }
}
