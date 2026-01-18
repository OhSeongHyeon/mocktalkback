package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Article reaction toggle request")
public record ArticleReactionToggleRequest(
    @Schema(description = "Reaction type (-1 dislike, 1 like)", example = "1")
    @NotNull
    Short reactionType
) {
}
