package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article file response")
public record ArticleFileResponse(
    @Schema(description = "Article file id", example = "1")
    Long id,

    @Schema(description = "File id", example = "100")
    Long fileId,

    @Schema(description = "Article id", example = "1")
    Long articleId,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
