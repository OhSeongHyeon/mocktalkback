package com.mocktalkback.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "계정 삭제 요청")
public record UserDeleteRequest(
    @Schema(description = "재확인 문구", example = "탈퇴")
    @NotBlank
    String confirmText
) {
}
