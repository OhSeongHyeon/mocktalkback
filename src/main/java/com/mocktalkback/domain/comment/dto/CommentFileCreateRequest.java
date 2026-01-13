package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Comment file create request")
public record CommentFileCreateRequest(
    @Schema(description = "File id", example = "100")
    @NotNull
    @Positive
    Long fileId,

    @Schema(description = "Comment id", example = "1")
    @NotNull
    @Positive
    Long commentId
) {
}
