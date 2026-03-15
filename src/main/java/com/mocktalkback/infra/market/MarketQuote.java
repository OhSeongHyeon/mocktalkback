package com.mocktalkback.infra.market;

import java.math.BigDecimal;
import java.time.Instant;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public record MarketQuote(
    MarketInstrumentCode instrumentCode,
    BigDecimal priceValue,
    Instant observedAt,
    String providerName
) {
}
