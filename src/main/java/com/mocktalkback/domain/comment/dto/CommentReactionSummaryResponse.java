package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comment reaction summary response")
public record CommentReactionSummaryResponse(
    @Schema(description = "Comment id", example = "1")
    Long commentId,

    @Schema(description = "Like count", example = "10")
    long likeCount,

    @Schema(description = "Dislike count", example = "2")
    long dislikeCount,

    @Schema(description = "My reaction (-1 dislike, 0 none, 1 like)", example = "1")
    short myReaction
) {
}
