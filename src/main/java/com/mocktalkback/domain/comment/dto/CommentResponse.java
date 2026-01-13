package com.mocktalkback.domain.comment.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comment response")
public record CommentResponse(
    @Schema(description = "Comment id", example = "1")
    Long id,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Article id", example = "1")
    Long articleId,

    @Schema(description = "Parent comment id", example = "10")
    Long parentCommentId,

    @Schema(description = "Root comment id", example = "10")
    Long rootCommentId,

    @Schema(description = "Depth", example = "0")
    int depth,

    @Schema(description = "Content", example = "This is a comment.")
    String content,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "Deleted at", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
