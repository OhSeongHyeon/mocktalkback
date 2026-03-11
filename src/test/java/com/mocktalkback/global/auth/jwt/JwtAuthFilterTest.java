package com.mocktalkback.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // 알림 실시간 스트림 경로에서도 쿼리 accessToken은 더 이상 인증에 사용하면 안 된다.
    @Test
    void doFilterInternal_ignores_query_access_token_for_notification_stream() throws Exception {
        // Given: JWT 파서와 필터
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtTokenProvider);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/realtime/notifications/stream");
        request.setServletPath("/api/realtime/notifications/stream");
        request.setParameter("accessToken", "query-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When: 필터 수행
        filter.doFilter(request, response, chain);

        // Then: query token은 무시되고 인증 정보가 설정되지 않음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verifyNoInteractions(jwtTokenProvider);
    }

    // 알림 스트림 외 경로에서는 쿼리 accessToken을 인증 토큰으로 사용하면 안 된다.
    @Test
    void doFilterInternal_ignores_query_access_token_for_other_paths() throws Exception {
        // Given: JWT 파서와 필터
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtTokenProvider);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications");
        request.setServletPath("/api/notifications");
        request.setParameter("accessToken", "query-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When: 필터 수행
        filter.doFilter(request, response, chain);

        // Then: query token은 무시되고 JWT 파싱도 호출되지 않음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verifyNoInteractions(jwtTokenProvider);
    }
}
