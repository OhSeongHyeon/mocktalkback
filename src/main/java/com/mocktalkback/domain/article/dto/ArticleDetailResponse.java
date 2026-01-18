package com.mocktalkback.domain.article.dto;

import java.time.Instant;
import java.util.List;

import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.role.type.ContentVisibility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article detail response")
public record ArticleDetailResponse(
    @Schema(description = "Article id", example = "1")
    Long id,

    @Schema(description = "Board info")
    ArticleBoardResponse board,

    @Schema(description = "Author id", example = "1")
    Long userId,

    @Schema(description = "Author name", example = "MockTalker")
    String authorName,

    @Schema(description = "Visibility", example = "PUBLIC")
    ContentVisibility visibility,

    @Schema(description = "Title", example = "Hello world")
    String title,

    @Schema(description = "Content(HTML)", example = "<p>Hello</p>")
    String content,

    @Schema(description = "Hit count", example = "0")
    long hit,

    @Schema(description = "Comment count", example = "0")
    long commentCount,

    @Schema(description = "Like count", example = "0")
    long likeCount,

    @Schema(description = "Dislike count", example = "0")
    long dislikeCount,

    @Schema(description = "My reaction (-1 dislike, 0 none, 1 like)", example = "0")
    short myReaction,

    @Schema(description = "Notice flag", example = "false")
    boolean notice,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "Attachments")
    List<FileResponse> attachments
) {
}
