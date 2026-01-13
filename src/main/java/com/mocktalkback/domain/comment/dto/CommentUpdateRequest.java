package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Comment update request")
public record CommentUpdateRequest(
    @Schema(description = "Content", example = "Updated comment.")
    @NotBlank
    String content
) {
}
