package com.mocktalkback.domain.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Board subscribe create request")
public record BoardSubscribeCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Board id", example = "1")
    @NotNull
    @Positive
    Long boardId
) {
}
