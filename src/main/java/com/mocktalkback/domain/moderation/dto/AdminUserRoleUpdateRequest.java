package com.mocktalkback.domain.moderation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 권한 변경 요청")
public record AdminUserRoleUpdateRequest(
    @Schema(description = "권한명", example = "ADMIN")
    @NotBlank
    String roleName
) {
}
