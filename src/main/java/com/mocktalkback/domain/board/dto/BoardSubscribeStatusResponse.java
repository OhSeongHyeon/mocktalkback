package com.mocktalkback.domain.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board subscribe status response")
public record BoardSubscribeStatusResponse(
    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Subscribed flag", example = "true")
    boolean subscribed
) {
}
