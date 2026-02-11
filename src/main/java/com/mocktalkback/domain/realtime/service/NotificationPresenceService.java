package com.mocktalkback.domain.realtime.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mocktalkback.domain.realtime.dto.NotificationPresenceUpdateRequest;
import com.mocktalkback.domain.realtime.type.NotificationPresenceViewType;

@Service
public class NotificationPresenceService {

    private static final long PRESENCE_TTL_MILLIS = 45_000L;
    private static final int MAX_SESSIONS_PER_USER = 8;

    private final Clock clock;
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, PresenceState>> userPresences;

    public NotificationPresenceService() {
        this(Clock.systemUTC());
    }

    NotificationPresenceService(Clock clock) {
        this.clock = clock;
        this.userPresences = new ConcurrentHashMap<>();
    }

    public void upsert(Long userId, NotificationPresenceUpdateRequest request) {
        ConcurrentHashMap<String, PresenceState> presences =
            userPresences.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        cleanupExpired(presences);
        evictOverflow(presences);

        PresenceState updated = new PresenceState(
            request.sessionId(),
            request.viewType(),
            request.articleId(),
            request.notificationPanelOpen(),
            clock.instant()
        );
        presences.put(request.sessionId(), updated);
    }

    public void remove(Long userId, String sessionId) {
        ConcurrentHashMap<String, PresenceState> presences = userPresences.get(userId);
        if (presences == null) {
            return;
        }

        presences.remove(sessionId);
        if (presences.isEmpty()) {
            userPresences.remove(userId);
        }
    }

    public boolean shouldSuppressUnreadCountPush(Long userId, Long articleId) {
        ConcurrentHashMap<String, PresenceState> presences = userPresences.get(userId);
        if (presences == null || presences.isEmpty()) {
            return false;
        }

        cleanupExpired(presences);
        if (presences.isEmpty()) {
            userPresences.remove(userId);
            return false;
        }

        for (PresenceState presence : presences.values()) {
            if (presence.notificationPanelOpen()) {
                return true;
            }
            if (articleId != null
                && presence.viewType() == NotificationPresenceViewType.ARTICLE_DETAIL
                && articleId.equals(presence.articleId())) {
                return true;
            }
        }
        return false;
    }

    // 끊긴 탭 heartbeat 누락 상태를 주기적으로 정리한다.
    @Scheduled(fixedDelay = 30_000L)
    public void cleanupExpired() {
        for (Map.Entry<Long, ConcurrentHashMap<String, PresenceState>> entry : userPresences.entrySet()) {
            ConcurrentHashMap<String, PresenceState> presences = entry.getValue();
            cleanupExpired(presences);
            if (presences.isEmpty()) {
                userPresences.remove(entry.getKey());
            }
        }
    }

    private void evictOverflow(ConcurrentHashMap<String, PresenceState> presences) {
        while (presences.size() >= MAX_SESSIONS_PER_USER) {
            String oldestSessionId = null;
            Instant oldestSeenAt = null;
            for (Map.Entry<String, PresenceState> entry : presences.entrySet()) {
                Instant seenAt = entry.getValue().lastSeenAt();
                if (oldestSeenAt == null || seenAt.isBefore(oldestSeenAt)) {
                    oldestSeenAt = seenAt;
                    oldestSessionId = entry.getKey();
                }
            }
            if (oldestSessionId == null) {
                return;
            }
            presences.remove(oldestSessionId);
        }
    }

    private void cleanupExpired(ConcurrentHashMap<String, PresenceState> presences) {
        Instant threshold = clock.instant().minusMillis(PRESENCE_TTL_MILLIS);
        presences.entrySet().removeIf(entry -> entry.getValue().lastSeenAt().isBefore(threshold));
    }

    private record PresenceState(
        String sessionId,
        NotificationPresenceViewType viewType,
        Long articleId,
        boolean notificationPanelOpen,
        Instant lastSeenAt
    ) {
    }
}
