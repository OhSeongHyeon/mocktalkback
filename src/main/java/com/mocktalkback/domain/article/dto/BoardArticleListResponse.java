package com.mocktalkback.domain.article.dto;

import java.util.List;

import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board article list response")
public record BoardArticleListResponse(
    @Schema(description = "Pinned articles (only first page)")
    List<ArticleSummaryResponse> pinned,

    @Schema(description = "Paged articles")
    PageResponse<ArticleSummaryResponse> page
) {
}
