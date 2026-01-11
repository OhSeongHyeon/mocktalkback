package com.mocktalkback.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access Token 응답")
public record TokenResponse(
    @Schema(description = "Access Token (JWT)")
    String accessToken,

    @Schema(description = "토큰 타입", example = "Bearer")
    String tokenType,

    @Schema(description = "만료까지 남은 초", example = "3600")
    long expiresInSec
) {

}
