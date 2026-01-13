package com.mocktalkback.domain.board.dto;

import java.time.Instant;

import com.mocktalkback.domain.board.type.BoardRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board member response")
public record BoardMemberResponse(
    @Schema(description = "Board member id", example = "1")
    Long id,

    @Schema(description = "User id", example = "1")
    Long userId,

    @Schema(description = "Board id", example = "1")
    Long boardId,

    @Schema(description = "Granted by user id", example = "2")
    Long grantedByUserId,

    @Schema(description = "Board role", example = "OWNER")
    BoardRole boardRole,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt
) {
}
