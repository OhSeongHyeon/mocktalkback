package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Article category update request")
public record ArticleCategoryUpdateRequest(
    @Schema(description = "Category name", example = "General")
    @NotBlank
    @Size(max = 48)
    String categoryName
) {
}
