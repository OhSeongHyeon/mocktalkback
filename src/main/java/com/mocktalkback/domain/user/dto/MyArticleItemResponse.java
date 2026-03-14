package com.mocktalkback.domain.user.dto;

import java.time.Instant;

import com.mocktalkback.domain.role.type.ContentVisibility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 게시글 목록 아이템")
public record MyArticleItemResponse(
    @Schema(description = "게시글 ID", example = "101")
    Long id,

    @Schema(description = "게시판 ID", example = "2")
    Long boardId,

    @Schema(description = "게시판 슬러그", example = "inquiry")
    String boardSlug,

    @Schema(description = "게시판 이름", example = "문의 게시판")
    String boardName,

    @Schema(description = "작성자 ID", example = "1")
    Long userId,

    @Schema(description = "작성자 이름", example = "Seed2")
    String authorName,

    @Schema(description = "카테고리 ID", example = "10")
    Long categoryId,

    @Schema(description = "공개 범위", example = "PUBLIC")
    ContentVisibility visibility,

    @Schema(description = "제목", example = "문의드립니다.")
    String title,

    @Schema(description = "조회수", example = "123")
    long hit,

    @Schema(description = "댓글 수", example = "4")
    long commentCount,

    @Schema(description = "좋아요 수", example = "8")
    long likeCount,

    @Schema(description = "싫어요 수", example = "1")
    long dislikeCount,

    @Schema(description = "공지 여부", example = "false")
    boolean notice,

    @Schema(description = "생성 시각", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "수정 시각", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "삭제 시각", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
