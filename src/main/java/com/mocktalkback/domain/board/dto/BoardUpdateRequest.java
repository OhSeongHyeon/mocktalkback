package com.mocktalkback.domain.board.dto;

import com.mocktalkback.domain.board.type.BoardVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Board update request")
public record BoardUpdateRequest(
    @Schema(description = "Board name", example = "Notice")
    @NotBlank
    @Size(max = 255)
    String boardName,

    @Schema(description = "Slug", example = "notice")
    @NotBlank
    @Size(max = 80)
    String slug,

    @Schema(description = "Description", example = "Board for announcements")
    String description,

    @Schema(description = "Visibility", example = "PUBLIC")
    @NotNull
    BoardVisibility visibility
) {
}
