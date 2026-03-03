package com.mocktalkback.domain.realtime.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mocktalkback.domain.realtime.config.RealtimeRedisProperties;
import com.mocktalkback.domain.realtime.dto.NotificationRealtimeEventResponse;
import com.mocktalkback.domain.realtime.type.NotificationRealtimeEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRealtimeSseService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final int MAX_EMITTERS_PER_USER = 2;

    private final RealtimeRedisPublisher realtimeRedisPublisher;
    private final RealtimeRedisProperties realtimeRedisProperties;
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId, String lastEventId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);

        ConcurrentHashMap<String, SseEmitter> emitters =
                userEmitters.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());

        evictIfNeeded(userId, emitters);
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitterId));
        emitter.onTimeout(() -> removeEmitter(userId, emitterId));
        emitter.onError(ex -> removeEmitter(userId, emitterId));

        Map<String, Object> connectPayload = new HashMap<>();
        connectPayload.put("emitterId", emitterId);
        connectPayload.put("lastEventId", lastEventId);

        publishToEmitter(
            emitterId,
            emitter,
            userId,
            NotificationRealtimeEventType.CONNECTED,
            connectPayload,
            UUID.randomUUID().toString(),
            Instant.now(),
            true
        );
        return emitter;
    }

    public void publishUnreadCountChanged(Long userId, long unreadCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("unreadCount", unreadCount);
        publish(userId, NotificationRealtimeEventType.UNREAD_COUNT_CHANGED, payload);
    }

    public void publish(Long userId, NotificationRealtimeEventType type, Object data) {
        String eventId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now();
        if (tryPublishRedis(userId, type, data, eventId, occurredAt)) {
            return;
        }
        publishLocal(userId, type, data, eventId, occurredAt);
    }

    void publishFromRedis(
        Long userId,
        NotificationRealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        publishLocal(userId, type, data, eventId, occurredAt);
    }

    private void publishLocal(
        Long userId,
        NotificationRealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        ConcurrentHashMap<String, SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            publishToEmitter(
                entry.getKey(),
                entry.getValue(),
                userId,
                type,
                data,
                eventId,
                occurredAt,
                false
            );
        }
    }

    // 주기적으로 heartbeat를 보내 끊어진 연결을 정리한다.
    @Scheduled(fixedDelay = 25_000L)
    public void publishHeartbeat() {
        for (Long userId : userEmitters.keySet()) {
            publishLocal(
                userId,
                NotificationRealtimeEventType.HEARTBEAT,
                null,
                UUID.randomUUID().toString(),
                Instant.now()
            );
        }
    }

    private void evictIfNeeded(Long userId, ConcurrentHashMap<String, SseEmitter> emitters) {
        while (emitters.size() >= MAX_EMITTERS_PER_USER) {
            String evictedEmitterId = emitters.keySet().stream().findFirst().orElse(null);
            if (evictedEmitterId == null) {
                return;
            }
            SseEmitter evictedEmitter = emitters.remove(evictedEmitterId);
            if (evictedEmitter != null) {
                evictedEmitter.complete();
            }
        }
        if (emitters.isEmpty()) {
            userEmitters.putIfAbsent(userId, emitters);
        }
    }

    private void publishToEmitter(
            String emitterId,
            SseEmitter emitter,
            Long userId,
            NotificationRealtimeEventType type,
            Object data,
            String eventId,
            Instant occurredAt,
            boolean removeOnFailure
    ) {
        NotificationRealtimeEventResponse event = new NotificationRealtimeEventResponse(
                eventId == null ? UUID.randomUUID().toString() : eventId,
                userId,
                type,
                occurredAt == null ? Instant.now() : occurredAt,
                data
        );

        try {
            emitter.send(SseEmitter.event()
                    .id(event.eventId())
                    .name(type.name().toLowerCase())
                    .data(event));
        } catch (Exception ex) {
            safeComplete(emitter, removeOnFailure, ex);
            removeEmitter(userId, emitterId);
        }
    }

    private boolean tryPublishRedis(
        Long userId,
        NotificationRealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        if (!realtimeRedisProperties.enabled()) {
            return false;
        }
        try {
            realtimeRedisPublisher.publishNotification(userId, type, data, eventId, occurredAt);
            return true;
        } catch (Exception ex) {
            if (!realtimeRedisProperties.fallbackEnabled()) {
                throw new IllegalStateException("알림 SSE Redis 발행에 실패했습니다.", ex);
            }
            log.warn("알림 SSE Redis 발행 실패로 로컬 fallback 경로를 사용합니다. userId={}", userId, ex);
            return false;
        }
    }

    private void safeComplete(SseEmitter emitter, boolean removeOnFailure, Exception ex) {
        try {
            if (removeOnFailure) {
                emitter.completeWithError(ex);
            } else {
                emitter.complete();
            }
        } catch (Exception ignore) {
            // 이미 닫힌 비동기 응답에서 complete()가 예외를 낼 수 있으므로 무시한다.
        }
    }

    private void removeEmitter(Long userId, String emitterId) {
        ConcurrentHashMap<String, SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitterId);
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }
}
