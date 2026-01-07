package com.mocktalkback.global.auth.oauth2;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.mocktalkback.domain.user.service.AuthService;
import com.mocktalkback.global.auth.jwt.JwtTokenProvider;
import com.mocktalkback.global.auth.jwt.RefreshCookie;
import com.mocktalkback.global.auth.jwt.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookie refreshCookie;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        Object principalObj = authentication.getPrincipal();

        log.info("[OAUTH2 SUCCESS] principal class={}", principalObj.getClass().getName());

        if (principalObj instanceof OAuth2User) {
            for (Map.Entry<String, Object> e : principal.getAttributes().entrySet()) {
                log.info("[OAUTH2 SUCCESS] attr {} = {}", e.getKey(), e.getValue());
            }
        } else {
            log.warn("[OAUTH2 SUCCESS] principal is not OAuth2User: {}", principalObj);
        }

        if (principal instanceof OidcUser oidcUser) {
            log.info("[OAUTH2 SUCCESS] principal=OidcUser, name={}", oidcUser.getName());

            oidcUser.getClaims().forEach((k, v) -> log.info("[OAUTH2 CLAIM] {} = {}", k, v));

        } else if (principal instanceof OAuth2User oAuth2User) {
            log.info("[OAUTH2 SUCCESS] principal=OAuth2User, name={}", oAuth2User.getName());

            oAuth2User.getAttributes().forEach((k, v) -> log.info("[OAUTH2 ATTR] {} = {}", k, v));

        } else {
            log.warn("[OAUTH2 SUCCESS] unknown principal type={}", principal.getClass().getName());
        }

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name"); // 없으면 null일 수 있음
        String sub = principal.getAttribute("sub"); // google user id (OIDC)

        if (email == null || email.isBlank()) {
            // 구글에서 email이 안 내려오면(권한/설정 문제) 로그인 실패 처리
            response.sendError(401, "Google email missing");
            return;
        }

        Long userId = authService.upsertGoogleUser(email, name, sub); // @Transactional 서비스

        var issued = refreshTokenService.issue(userId);
        var cookie = refreshCookie.create(issued.refreshToken(), jwtTokenProvider.refreshTtlSec());

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect("http://localhost:5173/login?oauth2=ok");
    }
}
