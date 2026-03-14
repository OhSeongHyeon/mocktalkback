package com.mocktalkback.domain.realtime.dto;

import java.time.Instant;

import com.mocktalkback.domain.realtime.type.RealtimeEventType;

public record BoardRealtimeRedisMessage(
    String eventId,
    Instant occurredAt,
    Long boardId,
    RealtimeEventType type,
    Object data
) {
}
