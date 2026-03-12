package com.mocktalkback.domain.file.service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    public Optional<FileViewTicketState> find(String ticket) {
        String redisKey = key(ticket);
        String storedFileId = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedFileId == null || storedFileId.isBlank()) {
            return Optional.empty();
        }
        Long expireMillis = stringRedisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
        if (expireMillis == null || expireMillis <= 0L) {
            return Optional.empty();
        }

        return Optional.of(new FileViewTicketState(
            Long.valueOf(storedFileId),
            Duration.ofMillis(expireMillis)
        ));
    }

    private String key(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }

    public record FileViewTicketState(
        Long fileId,
        Duration remainingTtl
    ) {
    }
}
