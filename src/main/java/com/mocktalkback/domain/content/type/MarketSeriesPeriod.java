package com.mocktalkback.domain.content.type;

public enum MarketSeriesPeriod {
    TEN_YEAR(3652),
    FIVE_YEAR(1826),
    THREE_YEAR(1095),
    YEAR(365),
    HALF_YEAR(183),
    QUARTER(92),
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
