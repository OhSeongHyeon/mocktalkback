package com.mocktalkback.domain.realtime.service;

import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocktalkback.domain.realtime.config.RealtimeRedisProperties;
import com.mocktalkback.domain.realtime.dto.BoardRealtimeRedisMessage;
import com.mocktalkback.domain.realtime.dto.NotificationRealtimeRedisMessage;
import com.mocktalkback.domain.realtime.type.NotificationRealtimeEventType;
import com.mocktalkback.domain.realtime.type.RealtimeEventType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RealtimeRedisPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RealtimeRedisProperties realtimeRedisProperties;

    public void publishNotification(
        Long userId,
        NotificationRealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        NotificationRealtimeRedisMessage message = new NotificationRealtimeRedisMessage(
            eventId,
            occurredAt,
            userId,
            type,
            data
        );
        publish(realtimeRedisProperties.notificationChannel(), message);
    }

    public void publishBoard(
        Long boardId,
        RealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        BoardRealtimeRedisMessage message = new BoardRealtimeRedisMessage(
            eventId,
            occurredAt,
            boardId,
            type,
            data
        );
        publish(realtimeRedisProperties.boardChannel(), message);
    }

    private void publish(String channel, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend(channel, json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("실시간 이벤트 직렬화에 실패했습니다.", ex);
        }
    }
}
