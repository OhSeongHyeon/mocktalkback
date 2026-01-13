package com.mocktalkback.domain.board.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board subscribe response")
public record BoardSubscribeResponse(
    @Schema(description = "Subscribe id", example = "1")
    Long id,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
