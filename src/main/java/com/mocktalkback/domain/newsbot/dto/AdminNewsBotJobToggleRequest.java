package com.mocktalkback.domain.newsbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "뉴스봇 잡 활성화 상태 변경 요청")
public record AdminNewsBotJobToggleRequest(
    @Schema(description = "활성화 여부", example = "true")
    @NotNull
    Boolean enabled
) {
}
