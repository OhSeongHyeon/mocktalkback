package com.mocktalkback.domain.comment.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comment file response")
public record CommentFileResponse(
    @Schema(description = "Comment file id", example = "1")
    Long id,

    @Schema(description = "File id", example = "100")
    Long fileId,

    @Schema(description = "Comment id", example = "1")
    Long commentId,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
