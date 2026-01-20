package com.mocktalkback.domain.search.dto;

import com.mocktalkback.global.common.dto.SliceResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "통합 검색 응답")
public record SearchResponse(
    @Schema(description = "게시판 검색 결과")
    SliceResponse<BoardSearchResponse> boards,

    @Schema(description = "게시글 검색 결과")
    SliceResponse<ArticleSearchResponse> articles,

    @Schema(description = "댓글 검색 결과")
    SliceResponse<CommentSearchResponse> comments,

    @Schema(description = "사용자 검색 결과")
    SliceResponse<UserSearchResponse> users
) {
}
