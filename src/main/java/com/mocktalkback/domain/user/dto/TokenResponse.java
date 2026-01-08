package com.mocktalkback.domain.user.dto;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresInSec
) {

}
