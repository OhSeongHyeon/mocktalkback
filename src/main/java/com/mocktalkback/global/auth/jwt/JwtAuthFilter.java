package com.mocktalkback.global.auth.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtTokenProvider jwt, ObjectMapper objectMapper) {
        this.jwt = jwt;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // /auth 하위 전부 + /error
        return path.equals("/error") || path.equals("/auth") || path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 토큰이 없으면 그냥 통과 (보호 API는 EntryPoint가 401 처리)
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        try {
            var c = jwt.parseClaims(token);

            Long userId = Long.valueOf(c.getSubject());
            String role = (String) c.get("role");

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            write401(response, request, "JWT_EXPIRED", "토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            write401(response, request, "JWT_INVALID", "토큰이 유효하지 않습니다.");
        }
    }

    private void write401(HttpServletResponse response, HttpServletRequest request, String code, String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
