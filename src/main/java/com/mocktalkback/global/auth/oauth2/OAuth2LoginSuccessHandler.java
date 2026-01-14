package com.mocktalkback.global.auth.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mocktalkback.global.auth.CookieUtil;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;
    private final OAuth2CodeService oAuth2CodeService;
    private final boolean defaultRememberMe;
    private final String redirectPath;

    public OAuth2LoginSuccessHandler(
            RefreshTokenService refreshTokenService,
            CookieUtil cookieUtil,
            OAuth2CodeService oAuth2CodeService,
            OAuth2Properties oAuth2Properties
    ) {
        this.refreshTokenService = refreshTokenService;
        this.cookieUtil = cookieUtil;
        this.oAuth2CodeService = oAuth2CodeService;
        this.defaultRememberMe = oAuth2Properties.isRememberMeDefault();
        this.redirectPath = oAuth2Properties.getRedirectPath();
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid OAuth2 principal");
            return;
        }

        Object rawUserId = oAuth2User.getAttributes().get("userId");
        if (rawUserId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userId");
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(String.valueOf(rawUserId));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid userId");
            return;
        }

        boolean rememberMe = resolveRememberMe(request);
        RefreshTokenService.IssuedRefresh issued = refreshTokenService.issue(userId, rememberMe);
        ResponseCookie refreshCookie = rememberMe
                ? cookieUtil.create(issued.refreshToken(), issued.refreshExpiresInSec())
                : cookieUtil.createSession(issued.refreshToken());
        ResponseCookie logoutCookie = rememberMe
                ? cookieUtil.createLogout(issued.refreshToken(), issued.refreshExpiresInSec())
                : cookieUtil.createLogoutSession(issued.refreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, logoutCookie.toString());

        String code = oAuth2CodeService.issue(userId);
        response.sendRedirect(buildRedirectUrl(code));
    }

    private boolean resolveRememberMe(HttpServletRequest request) {
        String raw = request.getParameter("rememberMe");
        if (StringUtils.hasText(raw)) {
            return Boolean.parseBoolean(raw);
        }
        return defaultRememberMe;
    }

    private String buildRedirectUrl(String code) {
        String base = StringUtils.hasText(redirectPath) ? redirectPath.trim() : "/oauth/callback";
        String separator = base.contains("?") ? "&" : "?";
        String encoded = URLEncoder.encode(code, StandardCharsets.UTF_8);
        return base + separator + "code=" + encoded;
    }
}
