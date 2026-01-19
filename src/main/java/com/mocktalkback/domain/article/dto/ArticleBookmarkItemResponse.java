package com.mocktalkback.domain.article.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article bookmark item response")
public record ArticleBookmarkItemResponse(
    @Schema(description = "Article id", example = "1")
    Long id,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Board slug", example = "notice")
    String boardSlug,

    @Schema(description = "Author id", example = "1")
    Long userId,

    @Schema(description = "Author name", example = "MockTalker")
    String authorName,

    @Schema(description = "Title", example = "Hello world")
    String title,

    @Schema(description = "Hit count", example = "0")
    long hit,

    @Schema(description = "Comment count", example = "0")
    long commentCount,

    @Schema(description = "Like count", example = "0")
    long likeCount,

    @Schema(description = "Dislike count", example = "0")
    long dislikeCount,

    @Schema(description = "Notice flag", example = "false")
    boolean notice,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
