package com.mocktalkback.domain.board.dto;

import com.mocktalkback.domain.board.type.BoardRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board member status response")
public record BoardMemberStatusResponse(
    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Member status", example = "PENDING")
    BoardRole memberStatus
) {
}
