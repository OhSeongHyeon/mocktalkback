package com.mocktalkback.global.auth.ticket;

public enum TicketChannel {
    REALTIME_CONNECT("realtime_connect"),
    RESOURCE_VIEW("resource_view"),
    RESOURCE_DOWNLOAD("resource_download");

    private final String redisKeySegment;

    TicketChannel(String redisKeySegment) {
        this.redisKeySegment = redisKeySegment;
    }

    public String redisKeySegment() {
        return redisKeySegment;
    }
}
