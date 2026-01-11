package com.mocktalkback.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "OAuth2 코드 교환 요청")
public record OAuth2CodeRequest(
        @Schema(description = "1회용 코드", example = "9958e615506f4be2b3b3ae2cb8d0bc55")
        @NotBlank
        String code
) {}
