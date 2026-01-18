package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Comment create request")
public record CommentCreateRequest(
    @Schema(description = "Content", example = "This is a comment.")
    @NotBlank
    String content
) {
}
