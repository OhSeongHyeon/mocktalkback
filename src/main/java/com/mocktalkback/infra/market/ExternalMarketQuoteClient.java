package com.mocktalkback.infra.market;

import java.util.Optional;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public interface ExternalMarketQuoteClient {
    Optional<MarketQuote> fetchQuote(MarketInstrumentCode instrumentCode);
}
