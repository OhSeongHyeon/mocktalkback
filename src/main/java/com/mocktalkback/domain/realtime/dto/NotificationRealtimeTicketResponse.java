package com.mocktalkback.domain.realtime.dto;

public record NotificationRealtimeTicketResponse(
    String ticket,
    long expiresInSec
) {
}
