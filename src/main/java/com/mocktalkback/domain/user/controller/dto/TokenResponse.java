package com.mocktalkback.domain.user.controller.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSec
) {}
