package com.mocktalkback.domain.content.dto;

import java.time.Instant;
import java.util.List;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public record AdminMarketImportResponse(
    Instant executedAt,
    String fileName,
    MarketInstrumentCode selectedInstrument,
    boolean unifiedFile,
    int totalCount,
    int createdCount,
    int updatedCount,
    int skippedCount,
    int failedCount,
    List<AdminMarketImportFailureResponse> failures
) {
}
