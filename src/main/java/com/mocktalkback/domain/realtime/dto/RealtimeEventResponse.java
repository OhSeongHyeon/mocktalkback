package com.mocktalkback.domain.realtime.dto;

import java.time.Instant;

import com.mocktalkback.domain.realtime.type.RealtimeEventType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "실시간 이벤트 응답")
public record RealtimeEventResponse(
    @Schema(description = "이벤트 아이디", example = "d7d3d77f-8b77-4c25-a4ad-5e3ce48cf8f1")
    String eventId,

    @Schema(description = "게시판 아이디", example = "1")
    Long boardId,

    @Schema(description = "이벤트 유형", example = "COMMENT_CHANGED")
    RealtimeEventType type,

    @Schema(description = "이벤트 발생 시각", example = "2026-02-11T12:00:00Z")
    Instant occurredAt,

    @Schema(description = "이벤트 데이터")
    Object data
) {
}

