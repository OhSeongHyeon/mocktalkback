package com.mocktalkback.domain.content.type;

import java.util.List;

public enum MarketInstrumentCode {
    USD_KRW("USD/KRW", MarketGroup.FX, "USD", "KRW", "원", "USDKRW=X", false),
    EUR_KRW("EUR/KRW", MarketGroup.FX, "EUR", "KRW", "원", "EURKRW=X", false),
    JPY_KRW("JPY/KRW", MarketGroup.FX, "JPY", "KRW", "원", "JPYKRW=X", false),
    XAU_USD("금 시세 (USD)", MarketGroup.METAL, "XAU", "USD", "달러", "GC=F", false),
    XAU_KRW("금 시세 (KRW)", MarketGroup.METAL, "XAU", "KRW", "원", null, true);

    private final String displayName;
    private final MarketGroup marketGroup;
    private final String baseCurrency;
    private final String quoteCurrency;
    private final String unitLabel;
    private final String yahooSymbol;
    private final boolean derived;

    MarketInstrumentCode(
        String displayName,
        MarketGroup marketGroup,
        String baseCurrency,
        String quoteCurrency,
        String unitLabel,
        String yahooSymbol,
        boolean derived
    ) {
        this.displayName = displayName;
        this.marketGroup = marketGroup;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.unitLabel = unitLabel;
        this.yahooSymbol = yahooSymbol;
        this.derived = derived;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MarketGroup getMarketGroup() {
        return marketGroup;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public String getYahooSymbol() {
        return yahooSymbol;
    }

    public boolean isDerived() {
        return derived;
    }

    public static List<MarketInstrumentCode> rawTargets() {
        return List.of(USD_KRW, EUR_KRW, JPY_KRW, XAU_USD);
    }

    public static List<MarketInstrumentCode> displayTargets() {
        return List.of(USD_KRW, EUR_KRW, JPY_KRW, XAU_USD, XAU_KRW);
    }
}