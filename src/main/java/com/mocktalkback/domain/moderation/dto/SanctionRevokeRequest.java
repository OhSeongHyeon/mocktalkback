package com.mocktalkback.domain.moderation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "제재 해제 요청")
public record SanctionRevokeRequest(
    @Schema(description = "해제 사유")
    @NotBlank
    String revokedReason
) {
}
