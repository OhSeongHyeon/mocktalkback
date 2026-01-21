package com.mocktalkback.domain.moderation.dto;

import com.mocktalkback.domain.moderation.type.ReportReasonCode;
import com.mocktalkback.domain.moderation.type.ReportTargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 접수 요청")
public record ReportCreateRequest(
    @Schema(description = "대상 유형", example = "ARTICLE")
    @NotNull
    ReportTargetType targetType,

    @Schema(description = "대상 식별자", example = "10")
    @NotNull
    Long targetId,

    @Schema(description = "신고 사유 코드", example = "SPAM")
    @NotNull
    ReportReasonCode reasonCode,

    @Schema(description = "신고 사유 상세")
    String reasonDetail
) {
}
