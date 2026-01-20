package com.mocktalkback.domain.search.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 검색 응답")
public record CommentSearchResponse(
    @Schema(description = "댓글 ID", example = "1")
    Long id,

    @Schema(description = "게시글 ID", example = "10")
    Long articleId,

    @Schema(description = "게시글 제목", example = "안녕하세요")
    String articleTitle,

    @Schema(description = "게시판 ID", example = "1")
    Long boardId,

    @Schema(description = "게시판 슬러그", example = "notice")
    String boardSlug,

    @Schema(description = "게시판명", example = "공지사항")
    String boardName,

    @Schema(description = "작성자 ID", example = "3")
    Long userId,

    @Schema(description = "작성자명", example = "MockTalker")
    String authorName,

    @Schema(description = "내용", example = "댓글 내용입니다.")
    String content,

    @Schema(description = "작성일", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
