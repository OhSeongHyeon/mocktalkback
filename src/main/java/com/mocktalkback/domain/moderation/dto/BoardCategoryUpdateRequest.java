package com.mocktalkback.domain.moderation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "게시판 카테고리 수정 요청")
public record BoardCategoryUpdateRequest(
    @Schema(description = "카테고리명", example = "공지")
    @NotBlank
    @Size(max = 48)
    String categoryName
) {
}
