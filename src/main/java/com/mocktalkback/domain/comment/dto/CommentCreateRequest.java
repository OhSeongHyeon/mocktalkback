package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Comment create request")
public record CommentCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Article id", example = "1")
    @NotNull
    @Positive
    Long articleId,

    @Schema(description = "Parent comment id", example = "10")
    @Positive
    Long parentCommentId,

    @Schema(description = "Root comment id", example = "10")
    @Positive
    Long rootCommentId,

    @Schema(description = "Depth", example = "0")
    @Min(0)
    int depth,

    @Schema(description = "Content", example = "This is a comment.")
    @NotBlank
    String content
) {
}
