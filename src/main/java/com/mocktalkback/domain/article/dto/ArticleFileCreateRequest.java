package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Article file create request")
public record ArticleFileCreateRequest(
    @Schema(description = "File id", example = "100")
    @NotNull
    @Positive
    Long fileId,

    @Schema(description = "Article id", example = "1")
    @NotNull
    @Positive
    Long articleId
) {
}
