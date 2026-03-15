package com.mocktalkback.domain.content.dto;

import java.time.Instant;
import java.util.List;

public record AdminMarketRefreshResponse(
    Instant executedAt,
    int totalCount,
    int createdCount,
    int updatedCount,
    int skippedCount,
    List<AdminMarketRefreshItemResponse> items
) {
}
