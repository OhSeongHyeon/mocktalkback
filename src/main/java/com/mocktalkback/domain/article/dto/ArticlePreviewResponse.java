package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article preview response")
public record ArticlePreviewResponse(
    @Schema(description = "미리보기 HTML", example = "<h1>Hello world</h1>")
    String content
) {
}
