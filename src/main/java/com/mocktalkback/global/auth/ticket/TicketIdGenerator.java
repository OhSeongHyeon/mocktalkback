package com.mocktalkback.global.auth.ticket;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TicketIdGenerator {

    public String generate(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            throw new IllegalArgumentException("ticket prefix가 비어 있습니다.");
        }
        return prefix.trim() + UUID.randomUUID().toString().replace("-", "");
    }
}
