package com.mocktalkback.domain.content.service;

import java.time.Instant;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public record MarketSnapshotWriteResult(
    MarketInstrumentCode instrumentCode,
    Instant observedAt,
    MarketSnapshotWriteStatus status
) {
}
