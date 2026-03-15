package com.mocktalkback.domain.content.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

record MarketSnapshotImportRow(
    int rowNumber,
    MarketInstrumentCode instrumentCode,
    Instant observedAt,
    BigDecimal priceValue
) {
}
