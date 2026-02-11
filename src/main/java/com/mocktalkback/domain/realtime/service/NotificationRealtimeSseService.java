package com.mocktalkback.domain.realtime.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mocktalkback.domain.realtime.dto.NotificationRealtimeEventResponse;
import com.mocktalkback.domain.realtime.type.NotificationRealtimeEventType;

@Service
public class NotificationRealtimeSseService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final int MAX_EMITTERS_PER_USER = 2;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> userEmitters;

    public NotificationRealtimeSseService() {
        this.userEmitters = new ConcurrentHashMap<>();
    }

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

        publishToEmitter(emitterId, emitter, userId, NotificationRealtimeEventType.CONNECTED, connectPayload, true);
        return emitter;
    }

    public void publishUnreadCountChanged(Long userId, long unreadCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("unreadCount", unreadCount);
        publish(userId, NotificationRealtimeEventType.UNREAD_COUNT_CHANGED, payload);
    }

    public void publish(Long userId, NotificationRealtimeEventType type, Object data) {
        ConcurrentHashMap<String, SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            publishToEmitter(entry.getKey(), entry.getValue(), userId, type, data, false);
        }
    }

    // 주기적으로 heartbeat를 보내 끊어진 연결을 정리한다.
    @Scheduled(fixedDelay = 25_000L)
    public void publishHeartbeat() {
        for (Long userId : userEmitters.keySet()) {
            publish(userId, NotificationRealtimeEventType.HEARTBEAT, null);
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
            boolean removeOnFailure
    ) {
        NotificationRealtimeEventResponse event = new NotificationRealtimeEventResponse(
                UUID.randomUUID().toString(),
                userId,
                type,
                Instant.now(),
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
