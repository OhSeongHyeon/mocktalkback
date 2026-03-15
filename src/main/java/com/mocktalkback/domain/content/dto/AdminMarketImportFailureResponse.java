package com.mocktalkback.domain.content.dto;

public record AdminMarketImportFailureResponse(
    int rowNumber,
    String message
) {
}
