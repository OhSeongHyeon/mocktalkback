package com.mocktalkback.domain.realtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationRealtimeSseServiceTest {

    // 구독 생성 시 사용자별 emitter 레지스트리에 연결이 등록되어야 한다.
    @Test
    void subscribe_registers_emitter_for_user() {
        // Given: 알림 SSE 서비스
        NotificationRealtimeSseService service = new NotificationRealtimeSseService();

        // When: 사용자 알림 스트림을 구독
        SseEmitter emitter = service.subscribe(10L, null);

        // Then: 사용자 레지스트리에 emitter가 등록됨
        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> registry = getRegistry(service);
        assertThat(registry).containsKey(10L);
        assertThat(registry.get(10L)).containsValue(emitter);
    }

    // 사용자당 최대 연결 수를 초과하면 기존 연결이 정리되어야 한다.
    @Test
    void subscribe_over_limit_evicts_previous_emitter() {
        // Given: 알림 SSE 서비스
        NotificationRealtimeSseService service = new NotificationRealtimeSseService();

        // When: 동일 사용자로 3회 구독
        service.subscribe(20L, null);
        service.subscribe(20L, null);
        service.subscribe(20L, null);

        // Then: 사용자당 연결 수는 상한(2)을 넘지 않음
        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> registry = getRegistry(service);
        assertThat(registry.get(20L)).hasSizeLessThanOrEqualTo(2);
    }

    // emitter 전송 예외가 발생하면 연결을 complete 처리해야 한다.
    @Test
    void publish_unread_count_changed_on_send_failure_completes_emitter() throws IOException {
        // Given: 전송 시 예외가 발생하는 emitter
        NotificationRealtimeSseService service = new NotificationRealtimeSseService();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("send failed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
        emitters.put("emitter-1", emitter);
        getRegistry(service).put(30L, emitters);

        // When: unread 변경 이벤트 발행
        service.publishUnreadCountChanged(30L, 5L);

        // Then: 예외 emitter는 complete 호출
        verify(emitter).complete();
    }

    // 구독자가 없는 사용자에게 이벤트를 발행해도 예외 없이 종료되어야 한다.
    @Test
    void publish_without_subscriber_does_not_throw() {
        // Given: 구독자가 없는 SSE 서비스
        NotificationRealtimeSseService service = new NotificationRealtimeSseService();

        // When & Then: 이벤트 발행 시 예외가 발생하지 않음
        assertThatCode(() -> service.publishUnreadCountChanged(999L, 1L))
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> getRegistry(
        NotificationRealtimeSseService service
    ) {
        Object field = ReflectionTestUtils.getField(service, "userEmitters");
        assertThat(field).isInstanceOf(ConcurrentHashMap.class);
        return (ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>>) field;
    }
}
