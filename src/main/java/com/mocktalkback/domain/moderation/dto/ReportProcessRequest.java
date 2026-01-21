package com.mocktalkback.domain.moderation.dto;

import com.mocktalkback.domain.moderation.type.ReportStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 처리 요청")
public record ReportProcessRequest(
    @Schema(description = "처리 상태", example = "RESOLVED")
    @NotNull
    ReportStatus status,

    @Schema(description = "처리 메모")
    String processedNote
) {
}
