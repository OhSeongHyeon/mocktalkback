package com.mocktalkback.domain.user.dto;

public record AuthTokens(
        String accessToken,
        long accessExpiresInSec,
        String refreshToken,
        long refreshExpiresInSec
) {}
