package com.mocktalkback.domain.realtime.dto;

import java.time.Instant;

import com.mocktalkback.domain.realtime.type.NotificationRealtimeEventType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 실시간 이벤트 응답")
public record NotificationRealtimeEventResponse(
    @Schema(description = "이벤트 아이디", example = "4bcd1c99-8a7e-4e8e-a7d8-6bb0fd6a2d12")
    String eventId,

    @Schema(description = "수신 사용자 아이디", example = "100")
    Long userId,

    @Schema(description = "이벤트 유형", example = "UNREAD_COUNT_CHANGED")
    NotificationRealtimeEventType type,

    @Schema(description = "이벤트 발생 시각", example = "2026-02-11T12:00:00Z")
    Instant occurredAt,

    @Schema(description = "이벤트 데이터")
    Object data
) {
}
