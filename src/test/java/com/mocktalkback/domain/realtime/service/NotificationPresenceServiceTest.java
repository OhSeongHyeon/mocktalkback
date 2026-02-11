package com.mocktalkback.domain.realtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.mocktalkback.domain.realtime.dto.NotificationPresenceUpdateRequest;
import com.mocktalkback.domain.realtime.type.NotificationPresenceViewType;

class NotificationPresenceServiceTest {

    // 같은 게시글 화면을 보고 있으면 unread push를 억제해야 한다.
    @Test
    void should_suppress_when_article_detail_presence_matches_article() {
        // Given: 게시글 상세 화면 presence가 기록된 사용자
        MutableClock clock = new MutableClock(Instant.parse("2026-02-11T12:00:00Z"));
        NotificationPresenceService service = new NotificationPresenceService(clock);
        service.upsert(1L, request("session-a", NotificationPresenceViewType.ARTICLE_DETAIL, 10L, false));

        // When: 동일 게시글에 대한 알림 push 여부를 판단
        boolean suppressed = service.shouldSuppressUnreadCountPush(1L, 10L);

        // Then: push는 억제되어야 함
        assertThat(suppressed).isTrue();
    }

    // 알림 패널이 열려 있으면 unread push를 억제해야 한다.
    @Test
    void should_suppress_when_notification_panel_open() {
        // Given: 알림 패널 open 상태 presence가 기록된 사용자
        MutableClock clock = new MutableClock(Instant.parse("2026-02-11T12:00:00Z"));
        NotificationPresenceService service = new NotificationPresenceService(clock);
        service.upsert(2L, request("session-b", NotificationPresenceViewType.OTHER, null, true));

        // When: 임의 알림 push 여부를 판단
        boolean suppressed = service.shouldSuppressUnreadCountPush(2L, null);

        // Then: push는 억제되어야 함
        assertThat(suppressed).isTrue();
    }

    // heartbeat TTL이 지난 presence는 억제 판단에서 제외되어야 한다.
    @Test
    void should_not_suppress_when_presence_expired() {
        // Given: presence가 오래되어 만료된 사용자
        MutableClock clock = new MutableClock(Instant.parse("2026-02-11T12:00:00Z"));
        NotificationPresenceService service = new NotificationPresenceService(clock);
        service.upsert(3L, request("session-c", NotificationPresenceViewType.ARTICLE_DETAIL, 20L, false));
        clock.plusSeconds(50);

        // When: 동일 게시글 알림 push 여부를 판단
        boolean suppressed = service.shouldSuppressUnreadCountPush(3L, 20L);

        // Then: 만료된 presence는 무시되어 억제되지 않아야 함
        assertThat(suppressed).isFalse();
    }

    private NotificationPresenceUpdateRequest request(
        String sessionId,
        NotificationPresenceViewType viewType,
        Long articleId,
        boolean notificationPanelOpen
    ) {
        return new NotificationPresenceUpdateRequest(
            sessionId,
            viewType,
            articleId,
            notificationPanelOpen
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void plusSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
