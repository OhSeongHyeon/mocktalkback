package com.mocktalkback.domain.moderation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "공지 상태 변경 요청")
public record BoardAdminNoticeUpdateRequest(
    @Schema(description = "공지 여부", example = "true")
    @NotNull
    Boolean notice
) {
}
