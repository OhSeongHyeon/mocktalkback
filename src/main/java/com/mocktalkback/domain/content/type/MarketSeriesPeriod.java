package com.mocktalkback.domain.content.type;

public enum MarketSeriesPeriod {
    YEAR(365),
    MONTH(30),
    WEEK(7),
    CUSTOM(0);

    private final int days;

    MarketSeriesPeriod(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
