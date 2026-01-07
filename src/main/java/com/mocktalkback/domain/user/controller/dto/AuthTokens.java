package com.mocktalkback.domain.user.controller.dto;

public record AuthTokens(String accessToken, long accessExpiresInSec, String refreshToken, long refreshExpiresInSec) {
    
}
