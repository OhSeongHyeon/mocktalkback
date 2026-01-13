package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Article bookmark create request")
public record ArticleBookmarkCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Article id", example = "1")
    @NotNull
    @Positive
    Long articleId
) {
}
