package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Article reaction create request")
public record ArticleReactionCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Article id", example = "1")
    @NotNull
    @Positive
    Long articleId,

    @Schema(description = "Reaction type (-1 dislike, 0 neutral, 1 like)", example = "1")
    @Min(-1)
    @Max(1)
    short reactionType
) {
}
