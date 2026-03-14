package com.mocktalkback.domain.user.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 댓글 목록 아이템")
public record MyCommentItemResponse(
    @Schema(description = "댓글 ID", example = "1001")
    Long id,

    @Schema(description = "작성자 ID", example = "1")
    Long userId,

    @Schema(description = "게시글 ID", example = "101")
    Long articleId,

    @Schema(description = "게시글 제목", example = "문의드립니다.")
    String articleTitle,

    @Schema(description = "게시판 ID", example = "2")
    Long boardId,

    @Schema(description = "게시판 슬러그", example = "inquiry")
    String boardSlug,

    @Schema(description = "게시판 이름", example = "문의 게시판")
    String boardName,

    @Schema(description = "작성자 이름", example = "Seed2")
    String authorName,

    @Schema(description = "부모 댓글 ID", example = "1000")
    Long parentCommentId,

    @Schema(description = "루트 댓글 ID", example = "1000")
    Long rootCommentId,

    @Schema(description = "댓글 깊이", example = "1")
    int depth,

    @Schema(description = "댓글 내용", example = "좋은 의견 감사합니다.")
    String content,

    @Schema(description = "생성 시각", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "수정 시각", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "삭제 시각", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
