package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Article category create request")
public record ArticleCategoryCreateRequest(
    @Schema(description = "Board id", example = "1")
    @NotNull
    @Positive
    Long boardId,

    @Schema(description = "Category name", example = "General")
    @NotBlank
    @Size(max = 48)
    String categoryName
) {
}
