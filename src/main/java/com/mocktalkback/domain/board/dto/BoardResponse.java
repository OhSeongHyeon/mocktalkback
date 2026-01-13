package com.mocktalkback.domain.board.dto;

import java.time.Instant;

import com.mocktalkback.domain.role.type.ContentVisibility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board response")
public record BoardResponse(
    @Schema(description = "Board id", example = "1")
    Long id,

    @Schema(description = "Board name", example = "Notice")
    String boardName,

    @Schema(description = "Slug", example = "notice")
    String slug,

    @Schema(description = "Description", example = "Board for announcements")
    String description,

    @Schema(description = "Visibility", example = "PUBLIC")
    ContentVisibility visibility,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "Deleted at", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
