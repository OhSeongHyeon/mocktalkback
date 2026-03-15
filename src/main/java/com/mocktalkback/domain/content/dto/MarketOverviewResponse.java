package com.mocktalkback.domain.content.dto;

import java.time.Instant;
import java.util.List;

public record MarketOverviewResponse(
    Instant lastObservedAt,
    List<MarketOverviewItemResponse> items
) {
}
