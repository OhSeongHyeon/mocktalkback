package com.mocktalkback.domain.user.dto;

public record AccessTokenResult(
        String accessToken,
        long accessExpiresInSec
) {}
