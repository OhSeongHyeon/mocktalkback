package com.mocktalkback.domain.realtime.dto;

import java.time.Instant;

import com.mocktalkback.domain.realtime.type.NotificationRealtimeEventType;

public record NotificationRealtimeRedisMessage(
    String eventId,
    Instant occurredAt,
    Long userId,
    NotificationRealtimeEventType type,
    Object data
) {
}
