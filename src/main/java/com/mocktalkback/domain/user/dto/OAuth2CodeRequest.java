package com.mocktalkback.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuth2CodeRequest(
        @NotBlank String code
) {}
