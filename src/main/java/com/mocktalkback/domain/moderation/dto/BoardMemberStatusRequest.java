package com.mocktalkback.domain.moderation.dto;

import com.mocktalkback.domain.board.type.BoardRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "게시판 멤버 상태 변경 요청")
public record BoardMemberStatusRequest(
    @Schema(description = "상태", example = "BANNED")
    @NotNull
    BoardRole boardRole
) {
}
