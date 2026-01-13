package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article reaction response")
public record ArticleReactionResponse(
    @Schema(description = "Reaction id", example = "1")
    Long id,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Article id", example = "1")
    Long articleId,

    @Schema(description = "Reaction type (-1 dislike, 0 neutral, 1 like)", example = "1")
    short reactionType,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
