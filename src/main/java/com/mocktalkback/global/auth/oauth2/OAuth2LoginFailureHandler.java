package com.mocktalkback.global.auth.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String DEFAULT_ERROR_CODE = "oauth2_login_failed";

    private final String redirectPath;

    public OAuth2LoginFailureHandler(
            OAuth2Properties oAuth2Properties
    ) {
        this.redirectPath = oAuth2Properties.getRedirectPath();
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = resolveErrorCode(exception);
        response.sendRedirect(buildRedirectUrl(errorCode));
    }

    private String resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            String errorCode = oAuth2Exception.getError().getErrorCode();
            if (StringUtils.hasText(errorCode)) {
                return errorCode;
            }
        }
        return DEFAULT_ERROR_CODE;
    }

    private String buildRedirectUrl(String errorCode) {
        String base = StringUtils.hasText(redirectPath) ? redirectPath.trim() : "/oauth/callback";
        String separator = base.contains("?") ? "&" : "?";
        String encoded = URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
        return base + separator + "error=" + encoded;
    }
}
