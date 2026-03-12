package com.mocktalkback.global.auth.ticket;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TicketRedisKeyBuilder {

    private static final String KEY_PREFIX = "ticket";

    public String build(TicketChannel channel, String ticketId) {
        if (channel == null) {
            throw new IllegalArgumentException("ticket channel이 비어 있습니다.");
        }
        if (!StringUtils.hasText(ticketId)) {
            throw new IllegalArgumentException("ticket id가 비어 있습니다.");
        }
        return KEY_PREFIX + ":" + channel.redisKeySegment() + ":" + ticketId.trim();
    }
}
