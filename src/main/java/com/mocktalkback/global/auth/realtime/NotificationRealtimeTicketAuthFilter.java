package com.mocktalkback.global.auth.realtime;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mocktalkback.domain.realtime.service.NotificationRealtimeTicketService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class NotificationRealtimeTicketAuthFilter extends OncePerRequestFilter {

    private static final String NOTIFICATION_STREAM_PATH = "/api/realtime/notifications/stream";

    private final NotificationRealtimeTicketService notificationRealtimeTicketService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equalsIgnoreCase(request.getMethod())
            || !NOTIFICATION_STREAM_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ticket = request.getParameter("ticket");
        if (ticket == null || ticket.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = notificationRealtimeTicketService.consume(ticket);
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.emptyList()
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            SecurityContextHolder.clearContext();
            log.debug("알림 SSE ticket 인증에 실패했습니다. path={}, reason={}", request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
