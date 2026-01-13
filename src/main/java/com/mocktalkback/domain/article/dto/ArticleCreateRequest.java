package com.mocktalkback.domain.article.dto;

import com.mocktalkback.domain.role.type.ContentVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Article create request")
public record ArticleCreateRequest(
    @Schema(description = "Board id", example = "1")
    @NotNull
    @Positive
    Long boardId,

    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Category id", example = "10")
    @Positive
    Long categoryId,

    @Schema(description = "Visibility", example = "PUBLIC")
    @NotNull
    ContentVisibility visibility,

    @Schema(description = "Title", example = "Hello world")
    @NotBlank
    @Size(max = 255)
    String title,

    @Schema(description = "Content", example = "This is a post.")
    @NotBlank
    String content,

    @Schema(description = "Notice flag", example = "false")
    boolean notice
) {
}
