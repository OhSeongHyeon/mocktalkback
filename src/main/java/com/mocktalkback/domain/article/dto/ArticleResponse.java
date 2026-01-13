package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import com.mocktalkback.domain.role.type.ContentVisibility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article response")
public record ArticleResponse(
    @Schema(description = "Article id", example = "1")
    Long id,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Category id", example = "10")
    Long categoryId,

    @Schema(description = "Visibility", example = "PUBLIC")
    ContentVisibility visibility,

    @Schema(description = "Title", example = "Hello world")
    String title,

    @Schema(description = "Content", example = "This is a post.")
    String content,

    @Schema(description = "Hit count", example = "0")
    long hit,

    @Schema(description = "Notice flag", example = "false")
    boolean notice,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "Deleted at", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
