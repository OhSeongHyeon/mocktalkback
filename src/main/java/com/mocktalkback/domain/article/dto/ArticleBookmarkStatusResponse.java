package com.mocktalkback.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article bookmark status response")
public record ArticleBookmarkStatusResponse(
    @Schema(description = "Article id", example = "1")
    Long articleId,

    @Schema(description = "Bookmarked flag", example = "true")
    boolean bookmarked
) {
}
