package com.mocktalkback.global.auth.oauth2;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            AuthenticationException exception
    ) throws java.io.IOException {

        if (exception instanceof OAuth2AuthenticationException oae) {
            var err = oae.getError();
            log.error("[OAUTH2 FAIL] code={}, desc={}, uri={}",
                    err.getErrorCode(), err.getDescription(), err.getUri(), oae);
        } else {
            log.error("[OAUTH2 FAIL] {}", exception.getMessage(), exception);
        }

        response.sendRedirect("http://localhost:5173/login?oauth2=fail");
    }
}
