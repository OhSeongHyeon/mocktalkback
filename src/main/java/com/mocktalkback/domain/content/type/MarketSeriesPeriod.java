package com.mocktalkback.domain.content.type;

public enum MarketSeriesPeriod {
    WEEK(7),
    MONTH(30);

    private final int days;

    MarketSeriesPeriod(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
