package com.mocktalkback.domain.article.dto;

import com.mocktalkback.domain.article.type.ArticleContentFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Article preview request")
public record ArticlePreviewRequest(
    @Schema(description = "작성 원본", example = "# Hello world")
    @NotBlank
    String contentSource,

    @Schema(description = "작성 원본 포맷", example = "MARKDOWN")
    @NotNull
    ArticleContentFormat contentFormat
) {
}
