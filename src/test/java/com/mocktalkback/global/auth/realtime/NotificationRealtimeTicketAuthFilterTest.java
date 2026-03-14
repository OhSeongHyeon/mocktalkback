package com.mocktalkback.global.auth.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mocktalkback.domain.realtime.service.NotificationRealtimeTicketService;

class NotificationRealtimeTicketAuthFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // 유효한 SSE ticket이면 stream 요청에 인증 정보를 설정해야 한다.
    @Test
    void doFilterInternal_authenticates_notification_stream_with_ticket() throws Exception {
        // Given: ticket 검증 서비스와 필터
        NotificationRealtimeTicketService ticketService = mock(NotificationRealtimeTicketService.class);
        when(ticketService.consume("ticket-123")).thenReturn(55L);
        NotificationRealtimeTicketAuthFilter filter = new NotificationRealtimeTicketAuthFilter(ticketService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/realtime/notifications/stream");
        request.setServletPath("/api/realtime/notifications/stream");
        request.setParameter("ticket", "ticket-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When: 필터 수행
        filter.doFilter(request, response, chain);

        // Then: SecurityContext에 ticket 사용자 정보가 설정됨
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(55L);
        verify(ticketService).consume("ticket-123");
    }

    // ticket이 없는 요청은 인증 정보를 설정하지 않고 그대로 통과해야 한다.
    @Test
    void doFilterInternal_skips_when_ticket_is_missing() throws Exception {
        // Given: ticket 검증 서비스와 필터
        NotificationRealtimeTicketService ticketService = mock(NotificationRealtimeTicketService.class);
        NotificationRealtimeTicketAuthFilter filter = new NotificationRealtimeTicketAuthFilter(ticketService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/realtime/notifications/stream");
        request.setServletPath("/api/realtime/notifications/stream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When: 필터 수행
        filter.doFilter(request, response, chain);

        // Then: 인증 정보는 비어 있고 ticket 검증도 호출되지 않음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verifyNoInteractions(ticketService);
    }
}
