package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 트렌딩 응답")
public record ArticleTrendingItemResponse(
    @Schema(description = "게시글 ID", example = "10")
    Long articleId,
    @Schema(description = "게시판 ID", example = "1")
    Long boardId,
    @Schema(description = "게시판 슬러그", example = "free")
    String boardSlug,
    @Schema(description = "작성자 ID", example = "2")
    Long userId,
    @Schema(description = "작성자 표시명", example = "mocktalk")
    String authorName,
    @Schema(description = "게시글 제목", example = "제목")
    String title,
    @Schema(description = "조회수", example = "12")
    long hit,
    @Schema(description = "댓글 수", example = "3")
    long commentCount,
    @Schema(description = "좋아요 수", example = "7")
    long likeCount,
    @Schema(description = "싫어요 수", example = "1")
    long dislikeCount,
    @Schema(description = "트렌딩 점수", example = "18.0")
    double trendScore,
    @Schema(description = "생성일시")
    Instant createdAt
) {
}
