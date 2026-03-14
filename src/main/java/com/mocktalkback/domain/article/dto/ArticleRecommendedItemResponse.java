package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 추천 응답")
public record ArticleRecommendedItemResponse(
    @Schema(description = "게시글 ID", example = "101")
    Long articleId,
    @Schema(description = "게시판 ID", example = "7")
    Long boardId,
    @Schema(description = "게시판 슬러그", example = "free")
    String boardSlug,
    @Schema(description = "게시판명", example = "자유게시판")
    String boardName,
    @Schema(description = "작성자 ID", example = "22")
    Long userId,
    @Schema(description = "작성자 표시명", example = "mocktalk")
    String authorName,
    @Schema(description = "게시글 제목", example = "추천 대상 글")
    String title,
    @Schema(description = "조회수", example = "19")
    long hit,
    @Schema(description = "댓글 수", example = "8")
    long commentCount,
    @Schema(description = "좋아요 수", example = "12")
    long likeCount,
    @Schema(description = "싫어요 수", example = "1")
    long dislikeCount,
    @Schema(description = "추천 점수", example = "14.5")
    double recommendationScore,
    @Schema(description = "추천 근거 문구", example = "북마크한 글과 비슷한 게시판 기반")
    String recommendationReason,
    @Schema(description = "개인화 추천 여부", example = "true")
    boolean personalized,
    @Schema(description = "생성일시")
    Instant createdAt
) {
}
