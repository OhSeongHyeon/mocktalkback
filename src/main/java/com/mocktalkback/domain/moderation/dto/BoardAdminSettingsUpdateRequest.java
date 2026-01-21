package com.mocktalkback.domain.moderation.dto;

import com.mocktalkback.domain.board.type.BoardVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "게시판 설정 수정 요청")
public record BoardAdminSettingsUpdateRequest(
    @Schema(description = "게시판명", example = "공지사항")
    @NotBlank
    @Size(max = 255)
    String boardName,

    @Schema(description = "게시판 설명", example = "공지와 안내를 제공합니다.")
    String description,

    @Schema(description = "공개 범위", example = "PUBLIC")
    @NotNull
    BoardVisibility visibility
) {
}
