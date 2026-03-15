package com.mocktalkback.domain.content.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketSeriesPointResponse(
    Instant timestamp,
    BigDecimal value
) {
}
