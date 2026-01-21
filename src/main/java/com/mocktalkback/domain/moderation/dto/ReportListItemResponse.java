package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.moderation.type.ReportReasonCode;
import com.mocktalkback.domain.moderation.type.ReportStatus;
import com.mocktalkback.domain.moderation.type.ReportTargetType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신고 목록 아이템")
public record ReportListItemResponse(
    @Schema(description = "신고 번호", example = "1")
    Long id,

    @Schema(description = "상태", example = "PENDING")
    ReportStatus status,

    @Schema(description = "대상 유형", example = "ARTICLE")
    ReportTargetType targetType,

    @Schema(description = "대상 식별자", example = "10")
    Long targetId,

    @Schema(description = "사유 코드", example = "SPAM")
    ReportReasonCode reasonCode,

    @Schema(description = "신고자 회원번호", example = "3")
    Long reporterUserId,

    @Schema(description = "대상 회원번호", example = "7")
    Long targetUserId,

    @Schema(description = "게시판 번호", example = "2")
    Long boardId,

    @Schema(description = "처리 일시", example = "2024-01-01T00:00:00Z")
    Instant processedAt,

    @Schema(description = "생성 일시", example = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
}
