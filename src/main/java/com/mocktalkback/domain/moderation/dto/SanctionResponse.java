package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.moderation.type.SanctionScopeType;
import com.mocktalkback.domain.moderation.type.SanctionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "제재 응답")
public record SanctionResponse(
    @Schema(description = "제재 번호", example = "10")
    Long id,

    @Schema(description = "대상 회원번호", example = "7")
    Long userId,

    @Schema(description = "제재 범위", example = "GLOBAL")
    SanctionScopeType scopeType,

    @Schema(description = "게시판 번호")
    Long boardId,

    @Schema(description = "제재 유형", example = "BAN")
    SanctionType sanctionType,

    @Schema(description = "제재 사유")
    String reason,

    @Schema(description = "제재 시작일시")
    Instant startsAt,

    @Schema(description = "제재 종료일시")
    Instant endsAt,

    @Schema(description = "연계 신고 번호")
    Long reportId,

    @Schema(description = "등록자 회원번호", example = "1")
    Long createdById,

    @Schema(description = "해제 일시")
    Instant revokedAt,

    @Schema(description = "해제자 회원번호")
    Long revokedById,

    @Schema(description = "해제 사유")
    String revokedReason,

    @Schema(description = "생성 일시")
    Instant createdAt,

    @Schema(description = "수정 일시")
    Instant updatedAt
) {
}
