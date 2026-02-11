package com.mocktalkback.global.auth.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    public JwtAuthFilter(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String path = request.getServletPath();
        final List<String> skip = List.of("/", 
            "/api/auth/join", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
            "/api/auth/oauth2/callback"
        );
        return skip.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String auth = resolveAuthorization(request);

        // 토큰이 없으면 그냥 통과 (보호 API는 EntryPoint가 401 처리)
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final int BEARER_STR_LEN = 7;
        String token = auth.substring(BEARER_STR_LEN);

        try {
            Claims c = jwt.parseClaims(token);
            Object typ = c.get("typ");
            if (!"access".equals(typ)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = Long.valueOf(c.getSubject());
            String role = (String) c.get("role"); // "USER", "ADMIN" ...
            if (role == null || role.isBlank()) {
                throw new JwtException("missing role");
            }

            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.warn("JWT parse failed: {} {}", request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
        } 

    }

    private String resolveAuthorization(HttpServletRequest request) {
        // 1) 기본 정책
        //    - 일반 API 요청은 Authorization 헤더(Bearer)를 우선 사용한다.
        //    - 헤더가 없으면 null을 반환하고, 보호 API는 이후 보안 체인에서 401 처리된다.
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader;
        }

        // 2) SSE(EventSource) 제약에 대한 제한적 우회
        //    - 브라우저 EventSource는 Authorization 같은 커스텀 헤더를 직접 붙일 수 없다.
        //    - 그래서 알림 SSE 구독 경로에서만 accessToken 쿼리 파라미터를 임시 허용한다.
        //    - 이 우회는 보안 노출면을 줄이기 위해 단일 경로로 엄격히 제한한다.
        //      (URL 쿼리는 접근 로그/프록시 로그에 남을 수 있음)
        String servletPath = request.getServletPath();
        if (!"/api/realtime/notifications/stream".equals(servletPath)) {
            return authorizationHeader;
        }

        // 3) 쿼리 토큰을 기존 Bearer 형식으로 합성
        //    - 아래 단계의 JWT 파싱/검증 로직을 재사용하기 위해 "Bearer <token>" 형태로 변환한다.
        //    - 즉 인증 규칙 자체를 별도로 만들지 않고, 기존 access 토큰 검증 체인에 합류시킨다.
        //
        //    향후 운영 보안 강화 방향:
        //    - 단기/1회용 SSE ticket 발급 -> stream 연결 시 ticket 검증/소모 구조로 전환 권장
        //    - 이렇게 하면 URL에 장기 JWT가 직접 노출되는 문제를 줄일 수 있다.
        String accessToken = request.getParameter("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            return authorizationHeader;
        }
        return "Bearer " + accessToken;
    }
}
