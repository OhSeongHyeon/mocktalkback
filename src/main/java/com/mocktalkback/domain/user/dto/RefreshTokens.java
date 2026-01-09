package com.mocktalkback.domain.user.dto;

public record RefreshTokens(
        String accessToken,
        long accessExpiresInSec,
        String refreshToken,
        long refreshExpiresInSec,
        boolean rememberMe
) {}
