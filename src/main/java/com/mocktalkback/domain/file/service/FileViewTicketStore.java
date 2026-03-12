package com.mocktalkback.domain.file.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileViewTicketStore {

    private static final String TICKET_KEY_PREFIX = "file:view:ticket:";

    private final StringRedisTemplate stringRedisTemplate;

    public void save(String ticket, Long fileId, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key(ticket), String.valueOf(fileId), ttl);
    }

    public Long consume(String ticket) {
        String storedFileId = stringRedisTemplate.opsForValue().getAndDelete(key(ticket));
        if (storedFileId == null || storedFileId.isBlank()) {
            return null;
        }
        return Long.valueOf(storedFileId);
    }

    private String key(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }
}
