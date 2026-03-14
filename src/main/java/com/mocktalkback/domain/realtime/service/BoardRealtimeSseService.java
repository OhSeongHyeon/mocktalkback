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
import com.mocktalkback.domain.realtime.dto.RealtimeEventResponse;
import com.mocktalkback.domain.realtime.type.RealtimeEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardRealtimeSseService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final RealtimeRedisPublisher realtimeRedisPublisher;
    private final RealtimeRedisProperties realtimeRedisProperties;
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> boardEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long boardId, String lastEventId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);

        ConcurrentHashMap<String, SseEmitter> emitters =
                boardEmitters.computeIfAbsent(boardId, key -> new ConcurrentHashMap<>());
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(boardId, emitterId));
        emitter.onTimeout(() -> removeEmitter(boardId, emitterId));
        emitter.onError(ex -> removeEmitter(boardId, emitterId));

        Map<String, Object> connectPayload = new HashMap<>();
        connectPayload.put("emitterId", emitterId);
        connectPayload.put("lastEventId", lastEventId);

        publishToEmitter(
            emitterId,
            emitter,
            boardId,
            RealtimeEventType.CONNECTED,
            connectPayload,
            UUID.randomUUID().toString(),
            Instant.now(),
            true
        );
        return emitter;
    }

    public void publishCommentChanged(Long boardId, Object data) {
        publish(boardId, RealtimeEventType.COMMENT_CHANGED, data);
    }

    public void publishReactionChanged(Long boardId, Object data) {
        publish(boardId, RealtimeEventType.REACTION_CHANGED, data);
    }

    public void publish(Long boardId, RealtimeEventType type, Object data) {
        String eventId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now();
        if (tryPublishRedis(boardId, type, data, eventId, occurredAt)) {
            return;
        }
        publishLocal(boardId, type, data, eventId, occurredAt);
    }

    void publishFromRedis(
        Long boardId,
        RealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        publishLocal(boardId, type, data, eventId, occurredAt);
    }

    private void publishLocal(
        Long boardId,
        RealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        ConcurrentHashMap<String, SseEmitter> emitters = boardEmitters.get(boardId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            publishToEmitter(
                entry.getKey(),
                entry.getValue(),
                boardId,
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
        for (Long boardId : boardEmitters.keySet()) {
            publishLocal(
                boardId,
                RealtimeEventType.HEARTBEAT,
                null,
                UUID.randomUUID().toString(),
                Instant.now()
            );
        }
    }

    private void publishToEmitter(
            String emitterId,
            SseEmitter emitter,
            Long boardId,
            RealtimeEventType type,
            Object data,
            String eventId,
            Instant occurredAt,
            boolean removeOnFailure
    ) {
        RealtimeEventResponse event = new RealtimeEventResponse(
                eventId == null ? UUID.randomUUID().toString() : eventId,
                boardId,
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
            removeEmitter(boardId, emitterId);
        }
    }

    private boolean tryPublishRedis(
        Long boardId,
        RealtimeEventType type,
        Object data,
        String eventId,
        Instant occurredAt
    ) {
        if (!realtimeRedisProperties.enabled()) {
            return false;
        }
        try {
            realtimeRedisPublisher.publishBoard(boardId, type, data, eventId, occurredAt);
            return true;
        } catch (Exception ex) {
            if (!realtimeRedisProperties.fallbackEnabled()) {
                throw new IllegalStateException("게시판 SSE Redis 발행에 실패했습니다.", ex);
            }
            log.warn("게시판 SSE Redis 발행 실패로 로컬 fallback 경로를 사용합니다. boardId={}", boardId, ex);
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

    private void removeEmitter(Long boardId, String emitterId) {
        ConcurrentHashMap<String, SseEmitter> emitters = boardEmitters.get(boardId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitterId);
        if (emitters.isEmpty()) {
            boardEmitters.remove(boardId);
        }
    }
}
