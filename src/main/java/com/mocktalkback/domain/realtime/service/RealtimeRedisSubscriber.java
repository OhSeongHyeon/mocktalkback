package com.mocktalkback.domain.realtime.service;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocktalkback.domain.realtime.config.RealtimeRedisProperties;
import com.mocktalkback.domain.realtime.dto.BoardRealtimeRedisMessage;
import com.mocktalkback.domain.realtime.dto.NotificationRealtimeRedisMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RealtimeRedisProperties realtimeRedisProperties;
    private final NotificationRealtimeSseService notificationRealtimeSseService;
    private final BoardRealtimeSseService boardRealtimeSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (!realtimeRedisProperties.enabled()) {
            return;
        }

        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        if (realtimeRedisProperties.notificationChannel().equals(channel)) {
            handleNotificationMessage(payload);
            return;
        }
        if (realtimeRedisProperties.boardChannel().equals(channel)) {
            handleBoardMessage(payload);
        }
    }

    private void handleNotificationMessage(String payload) {
        try {
            NotificationRealtimeRedisMessage message = objectMapper.readValue(
                payload,
                NotificationRealtimeRedisMessage.class
            );
            notificationRealtimeSseService.publishFromRedis(
                message.userId(),
                message.type(),
                message.data(),
                message.eventId(),
                message.occurredAt()
            );
        } catch (JsonProcessingException ex) {
            log.warn("notification redis 메시지 파싱에 실패했습니다. payload={}", payload, ex);
        }
    }

    private void handleBoardMessage(String payload) {
        try {
            BoardRealtimeRedisMessage message = objectMapper.readValue(
                payload,
                BoardRealtimeRedisMessage.class
            );
            boardRealtimeSseService.publishFromRedis(
                message.boardId(),
                message.type(),
                message.data(),
                message.eventId(),
                message.occurredAt()
            );
        } catch (JsonProcessingException ex) {
            log.warn("board redis 메시지 파싱에 실패했습니다. payload={}", payload, ex);
        }
    }
}
