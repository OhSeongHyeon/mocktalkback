package com.mocktalkback.domain.realtime.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationRealtimeTicketStore {

    private static final String TICKET_KEY_PREFIX = "realtime:notification:ticket:";

    private final StringRedisTemplate stringRedisTemplate;

    public void save(String ticket, Long userId, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key(ticket), String.valueOf(userId), ttl);
    }

    public Long consume(String ticket) {
        String storedUserId = stringRedisTemplate.opsForValue().getAndDelete(key(ticket));
        if (storedUserId == null || storedUserId.isBlank()) {
            return null;
        }
        return Long.valueOf(storedUserId);
    }

    private String key(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }
}
