package com.mocktalkback.domain.moderation.dto;

import com.mocktalkback.domain.board.type.BoardRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "게시판 멤버 역할 변경 요청")
public record BoardMemberRoleUpdateRequest(
    @Schema(description = "변경할 역할", example = "MODERATOR")
    @NotNull
    BoardRole boardRole
) {
}
