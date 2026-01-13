package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article bookmark response")
public record ArticleBookmarkResponse(
    @Schema(description = "Bookmark id", example = "1")
    Long id,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Article id", example = "1")
    Long articleId,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
