package com.mocktalkback.domain.content.dto;

import java.time.Instant;
import java.util.List;

import com.mocktalkback.domain.content.type.MarketGroup;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.domain.content.type.MarketSeriesPeriod;

public record MarketSeriesResponse(
    MarketInstrumentCode instrumentCode,
    String displayName,
    MarketGroup marketGroup,
    String unitLabel,
    MarketSeriesPeriod period,
    Instant lastObservedAt,
    List<MarketSeriesPointResponse> points
) {
}
