package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.moderation.type.SanctionScopeType;
import com.mocktalkback.domain.moderation.type.SanctionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "제재 등록 요청")
public record SanctionCreateRequest(
    @Schema(description = "대상 회원번호", example = "7")
    @NotNull
    Long userId,

    @Schema(description = "제재 범위", example = "GLOBAL")
    @NotNull
    SanctionScopeType scopeType,

    @Schema(description = "게시판 번호(BOARD 범위일 때)")
    Long boardId,

    @Schema(description = "제재 유형", example = "SUSPEND")
    @NotNull
    SanctionType sanctionType,

    @Schema(description = "제재 사유")
    @NotBlank
    String reason,

    @Schema(description = "제재 시작일시")
    Instant startsAt,

    @Schema(description = "제재 종료일시")
    Instant endsAt,

    @Schema(description = "연계 신고 번호")
    Long reportId
) {
}
