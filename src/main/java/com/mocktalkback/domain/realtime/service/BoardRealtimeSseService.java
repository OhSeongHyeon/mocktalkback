package com.mocktalkback.domain.realtime.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mocktalkback.domain.realtime.dto.RealtimeEventResponse;
import com.mocktalkback.domain.realtime.type.RealtimeEventType;

@Service
public class BoardRealtimeSseService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> boardEmitters;

    public BoardRealtimeSseService() {
        this.boardEmitters = new ConcurrentHashMap<>();
    }

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

        publishToEmitter(emitter, boardId, RealtimeEventType.CONNECTED, connectPayload, true);
        return emitter;
    }

    public void publishCommentChanged(Long boardId, Object data) {
        publish(boardId, RealtimeEventType.COMMENT_CHANGED, data);
    }

    public void publishReactionChanged(Long boardId, Object data) {
        publish(boardId, RealtimeEventType.REACTION_CHANGED, data);
    }

    public void publish(Long boardId, RealtimeEventType type, Object data) {
        ConcurrentHashMap<String, SseEmitter> emitters = boardEmitters.get(boardId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            publishToEmitter(entry.getValue(), boardId, type, data, false);
        }
    }

    // 주기적으로 heartbeat를 보내 끊어진 연결을 정리한다.
    @Scheduled(fixedDelay = 25_000L)
    public void publishHeartbeat() {
        for (Long boardId : boardEmitters.keySet()) {
            publish(boardId, RealtimeEventType.HEARTBEAT, null);
        }
    }

    private void publishToEmitter(
            SseEmitter emitter,
            Long boardId,
            RealtimeEventType type,
            Object data,
            boolean removeOnFailure
    ) {
        RealtimeEventResponse event = new RealtimeEventResponse(
                UUID.randomUUID().toString(),
                boardId,
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
            if (removeOnFailure) {
                emitter.completeWithError(ex);
            } else {
                emitter.complete();
            }
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

