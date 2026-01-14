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
    private static final String SAME_SITE = "Lax";
    private static final String REFRESH_PATH = "/api/auth/refresh";
    private static final String LOGOUT_PATH = "/api/auth/logout";

    public ResponseCookie create(String refreshToken, long maxAgeSec) {
        return buildCookie(refreshToken, maxAgeSec, REFRESH_PATH);
    }

    public ResponseCookie createSession(String refreshToken) {
        return buildSessionCookie(refreshToken, REFRESH_PATH);
    }

    public ResponseCookie clear() {
        return clearCookie(REFRESH_PATH);
    }

    public ResponseCookie createLogout(String refreshToken, long maxAgeSec) {
        return buildCookie(refreshToken, maxAgeSec, LOGOUT_PATH);
    }

    public ResponseCookie createLogoutSession(String refreshToken) {
        return buildSessionCookie(refreshToken, LOGOUT_PATH);
    }

    public ResponseCookie clearLogout() {
        return clearCookie(LOGOUT_PATH);
    }

    private ResponseCookie buildCookie(String refreshToken, long maxAgeSec, String path) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(maxAgeSec)
                .build();
    }

    private ResponseCookie buildSessionCookie(String refreshToken, String path) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(path)
                .build();
    }

    private ResponseCookie clearCookie(String path) {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(0)
                .build();
    }
}
