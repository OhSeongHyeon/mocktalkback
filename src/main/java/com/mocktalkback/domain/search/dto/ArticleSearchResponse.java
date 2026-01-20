package com.mocktalkback.domain.search.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 검색 응답")
public record ArticleSearchResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long id,

    @Schema(description = "게시판 ID", example = "1")
    Long boardId,

    @Schema(description = "게시판 슬러그", example = "notice")
    String boardSlug,

    @Schema(description = "게시판명", example = "공지사항")
    String boardName,

    @Schema(description = "작성자 ID", example = "1")
    Long userId,

    @Schema(description = "작성자명", example = "MockTalker")
    String authorName,

    @Schema(description = "제목", example = "안녕하세요")
    String title,

    @Schema(description = "조회수", example = "0")
    long hit,

    @Schema(description = "댓글 수", example = "0")
    long commentCount,

    @Schema(description = "좋아요 수", example = "0")
    long likeCount,

    @Schema(description = "싫어요 수", example = "0")
    long dislikeCount,

    @Schema(description = "공지 여부", example = "false")
    boolean notice,

    @Schema(description = "작성일", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
