package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article category response")
public record ArticleCategoryResponse(
    @Schema(description = "Category id", example = "10")
    Long id,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Category name", example = "General")
    String categoryName,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
