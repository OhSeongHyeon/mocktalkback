package com.mocktalkback.domain.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Board file create request")
public record BoardFileCreateRequest(
    @Schema(description = "File id", example = "100")
    @NotNull
    @Positive
    Long fileId,

    @Schema(description = "Board id", example = "1")
    @NotNull
    @Positive
    Long boardId
) {
}
