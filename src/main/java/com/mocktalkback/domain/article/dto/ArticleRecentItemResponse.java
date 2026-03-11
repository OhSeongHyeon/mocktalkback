package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 최근 게시글 응답")
public record ArticleRecentItemResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long id,

    @Schema(description = "게시판 ID", example = "1")
    Long boardId,

    @Schema(description = "게시판 슬러그", example = "free")
    String boardSlug,

    @Schema(description = "게시판명", example = "자유게시판")
    String boardName,

    @Schema(description = "작성자 ID", example = "10")
    Long userId,

    @Schema(description = "작성자명", example = "MockTalker")
    String authorName,

    @Schema(description = "제목", example = "첫 글입니다")
    String title,

    @Schema(description = "본문 미리보기", example = "본문 미리보기 텍스트")
    String previewText,

    @Schema(description = "댓글 수", example = "3")
    long commentCount,

    @Schema(description = "좋아요 수", example = "7")
    long likeCount,

    @Schema(description = "조회수", example = "12")
    long hit,

    @Schema(description = "작성일", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
