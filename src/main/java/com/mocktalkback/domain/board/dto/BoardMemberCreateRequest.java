package com.mocktalkback.domain.board.dto;

import com.mocktalkback.domain.board.type.BoardRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Board member create request")
public record BoardMemberCreateRequest(
    @Schema(description = "User id", example = "1")
    @NotNull
    @Positive
    Long userId,

    @Schema(description = "Board id", example = "1")
    @NotNull
    @Positive
    Long boardId,

    @Schema(description = "Granted by user id", example = "2")
    @Positive
    Long grantedByUserId,

    @Schema(description = "Board role", example = "OWNER")
    @NotNull
    BoardRole boardRole
) {
}
