package com.mocktalkback.domain.content.dto;

import java.time.Instant;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public record AdminMarketRefreshItemResponse(
    MarketInstrumentCode instrumentCode,
    Instant observedAt,
    String status
) {
}
