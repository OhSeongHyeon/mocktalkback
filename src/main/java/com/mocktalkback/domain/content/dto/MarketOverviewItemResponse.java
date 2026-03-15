package com.mocktalkback.domain.content.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mocktalkback.domain.content.type.MarketGroup;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public record MarketOverviewItemResponse(
    MarketInstrumentCode instrumentCode,
    String displayName,
    MarketGroup marketGroup,
    String baseCurrency,
    String quoteCurrency,
    String unitLabel,
    BigDecimal priceValue,
    BigDecimal changeValue,
    BigDecimal changeRate,
    Instant observedAt
) {
}
