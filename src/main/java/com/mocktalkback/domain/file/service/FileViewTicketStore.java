package com.mocktalkback.domain.file.service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mocktalkback.global.auth.ticket.TicketChannel;
import com.mocktalkback.global.auth.ticket.TicketRedisKeyBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileViewTicketStore {

    private static final TicketChannel TICKET_CHANNEL = TicketChannel.RESOURCE_VIEW;

    private final StringRedisTemplate stringRedisTemplate;
    private final TicketRedisKeyBuilder ticketRedisKeyBuilder;

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
        return ticketRedisKeyBuilder.build(TICKET_CHANNEL, ticket);
    }

    public record FileViewTicketState(
        Long fileId,
        Duration remainingTtl
    ) {
    }
}
