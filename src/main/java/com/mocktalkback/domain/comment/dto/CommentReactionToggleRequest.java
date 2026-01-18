package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Comment reaction toggle request")
public record CommentReactionToggleRequest(
    @Schema(description = "Reaction type (-1 dislike, 1 like)", example = "1")
    @NotNull
    Short reactionType
) {
}
