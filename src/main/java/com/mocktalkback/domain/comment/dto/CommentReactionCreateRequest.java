package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Comment reaction create request")
public record CommentReactionCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Comment id", example = "1")
    @NotNull
    @Positive
    Long commentId,

    @Schema(description = "Reaction type (-1 dislike, 0 neutral, 1 like)", example = "1")
    @Min(-1)
    @Max(1)
    short reactionType
) {
}
