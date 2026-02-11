package com.mocktalkback.domain.realtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class BoardRealtimeSseServiceTest {

    // 구독 생성 시 게시판별 emitter 레지스트리에 연결이 등록되어야 한다.
    @Test
    void subscribe_registers_emitter_for_board() {
        // Given: 실시간 SSE 서비스
        BoardRealtimeSseService service = new BoardRealtimeSseService();

        // When: 게시판 스트림을 구독
        SseEmitter emitter = service.subscribe(1L, null);

        // Then: 게시판 레지스트리에 emitter가 등록됨
        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> registry = getRegistry(service);
        assertThat(registry).containsKey(1L);
        assertThat(registry.get(1L)).containsValue(emitter);
    }

    // emitter 전송 예외가 발생하면 연결을 complete 처리해야 한다.
    @Test
    void publish_comment_changed_on_send_failure_completes_emitter() throws IOException {
        // Given: 전송 시 예외가 발생하는 emitter
        BoardRealtimeSseService service = new BoardRealtimeSseService();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("send failed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
        emitters.put("emitter-1", emitter);
        getRegistry(service).put(1L, emitters);

        // When: 댓글 변경 이벤트 발행
        service.publishCommentChanged(1L, Map.of("commentId", 100L));

        // Then: 예외 emitter는 complete 호출
        verify(emitter).complete();
    }

    // 구독자가 없는 게시판에 이벤트를 발행해도 예외 없이 종료되어야 한다.
    @Test
    void publish_without_subscriber_does_not_throw() {
        // Given: 구독자가 없는 SSE 서비스
        BoardRealtimeSseService service = new BoardRealtimeSseService();

        // When & Then: 이벤트 발행 시 예외가 발생하지 않음
        assertThatCode(() -> service.publishReactionChanged(999L, Map.of("articleId", 10L)))
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> getRegistry(
        BoardRealtimeSseService service
    ) {
        Object field = ReflectionTestUtils.getField(service, "boardEmitters");
        assertThat(field).isInstanceOf(ConcurrentHashMap.class);
        return (ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>>) field;
    }
}

