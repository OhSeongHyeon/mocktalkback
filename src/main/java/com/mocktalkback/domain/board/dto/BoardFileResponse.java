package com.mocktalkback.domain.board.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board file response")
public record BoardFileResponse(
    @Schema(description = "Board file id", example = "1")
    Long id,

    @Schema(description = "File id", example = "100")
    Long fileId,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
