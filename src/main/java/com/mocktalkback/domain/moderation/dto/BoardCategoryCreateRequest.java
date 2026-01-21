package com.mocktalkback.domain.moderation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "게시판 카테고리 생성 요청")
public record BoardCategoryCreateRequest(
    @Schema(description = "카테고리명", example = "자유")
    @NotBlank
    @Size(max = 48)
    String categoryName
) {
}
