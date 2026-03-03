package com.mocktalkback.domain.realtime.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mocktalkback.domain.realtime.config.RealtimeRedisProperties;
import com.mocktalkback.domain.realtime.dto.NotificationPresenceUpdateRequest;
import com.mocktalkback.domain.realtime.service.NotificationPresenceRedisStore.RedisPresenceState;
import com.mocktalkback.domain.realtime.type.NotificationPresenceViewType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationPresenceService {

    private final NotificationPresenceRedisStore notificationPresenceRedisStore;
    private final RealtimeRedisProperties realtimeRedisProperties;
    private final Clock clock;
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, PresenceState>> userPresences;

    @Autowired
    public NotificationPresenceService(
        NotificationPresenceRedisStore notificationPresenceRedisStore,
        RealtimeRedisProperties realtimeRedisProperties
    ) {
        this(notificationPresenceRedisStore, realtimeRedisProperties, Clock.systemUTC());
    }

    NotificationPresenceService(
        NotificationPresenceRedisStore notificationPresenceRedisStore,
        RealtimeRedisProperties realtimeRedisProperties,
        Clock clock
    ) {
        this.notificationPresenceRedisStore = notificationPresenceRedisStore;
        this.realtimeRedisProperties = realtimeRedisProperties;
        this.clock = clock;
        this.userPresences = new ConcurrentHashMap<>();
    }

    public void upsert(Long userId, NotificationPresenceUpdateRequest request) {
        if (tryRedisUpsert(userId, request)) {
            return;
        }
        upsertInMemory(userId, request);
    }

    private void upsertInMemory(Long userId, NotificationPresenceUpdateRequest request) {
        ConcurrentHashMap<String, PresenceState> presences =
            userPresences.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        cleanupExpiredInMemory(presences);
        evictOverflowInMemory(presences);

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
        if (tryRedisRemove(userId, sessionId)) {
            return;
        }
        removeInMemory(userId, sessionId);
    }

    private void removeInMemory(Long userId, String sessionId) {
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
        Boolean redisResult = tryRedisShouldSuppress(userId, articleId);
        if (redisResult != null) {
            return redisResult;
        }

        ConcurrentHashMap<String, PresenceState> presences = userPresences.get(userId);
        if (presences == null || presences.isEmpty()) {
            return false;
        }

        cleanupExpiredInMemory(presences);
        if (presences.isEmpty()) {
            userPresences.remove(userId);
            return false;
        }

        for (PresenceState presence : presences.values()) {
            if (presence.notificationPanelOpen()) {
                return true;
            }
            if (isArticleDetailPresence(presence.viewType(), presence.articleId(), articleId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isViewingArticleDetail(Long userId, Long articleId) {
        if (articleId == null) {
            return false;
        }

        Boolean redisResult = tryRedisIsViewingArticleDetail(userId, articleId);
        if (redisResult != null) {
            return redisResult;
        }

        ConcurrentHashMap<String, PresenceState> presences = userPresences.get(userId);
        if (presences == null || presences.isEmpty()) {
            return false;
        }

        cleanupExpiredInMemory(presences);
        if (presences.isEmpty()) {
            userPresences.remove(userId);
            return false;
        }

        for (PresenceState presence : presences.values()) {
            if (isArticleDetailPresence(presence.viewType(), presence.articleId(), articleId)) {
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
            cleanupExpiredInMemory(presences);
            if (presences.isEmpty()) {
                userPresences.remove(entry.getKey());
            }
        }
    }

    private void evictOverflowInMemory(ConcurrentHashMap<String, PresenceState> presences) {
        int maxSessions = resolvePresenceMaxSessions();
        while (presences.size() >= maxSessions) {
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

    private void cleanupExpiredInMemory(ConcurrentHashMap<String, PresenceState> presences) {
        Duration ttl = resolvePresenceTtl();
        Instant threshold = clock.instant().minus(ttl);
        presences.entrySet().removeIf(entry -> entry.getValue().lastSeenAt().isBefore(threshold));
    }

    private boolean tryRedisUpsert(Long userId, NotificationPresenceUpdateRequest request) {
        if (!isRedisPresenceEnabled()) {
            return false;
        }
        try {
            notificationPresenceRedisStore.upsert(userId, request, clock.instant());
            return true;
        } catch (Exception ex) {
            if (!isFallbackEnabled()) {
                throw new IllegalStateException("알림 presence Redis 저장에 실패했습니다.", ex);
            }
            log.warn("알림 presence Redis 저장 실패로 메모리 fallback을 사용합니다. userId={}", userId, ex);
            return false;
        }
    }

    private boolean tryRedisRemove(Long userId, String sessionId) {
        if (!isRedisPresenceEnabled()) {
            return false;
        }
        try {
            notificationPresenceRedisStore.remove(userId, sessionId);
            return true;
        } catch (Exception ex) {
            if (!isFallbackEnabled()) {
                throw new IllegalStateException("알림 presence Redis 삭제에 실패했습니다.", ex);
            }
            log.warn("알림 presence Redis 삭제 실패로 메모리 fallback을 사용합니다. userId={}", userId, ex);
            return false;
        }
    }

    private Boolean tryRedisShouldSuppress(Long userId, Long articleId) {
        if (!isRedisPresenceEnabled()) {
            return null;
        }
        try {
            return shouldSuppressFromRedis(userId, articleId);
        } catch (Exception ex) {
            if (!isFallbackEnabled()) {
                throw new IllegalStateException("알림 presence Redis 조회에 실패했습니다.", ex);
            }
            log.warn("알림 presence Redis 조회 실패로 메모리 fallback을 사용합니다. userId={}", userId, ex);
            return null;
        }
    }

    private boolean shouldSuppressFromRedis(Long userId, Long articleId) {
        List<RedisPresenceState> presences = notificationPresenceRedisStore.findByUserId(userId);
        if (presences.isEmpty()) {
            return false;
        }
        for (RedisPresenceState presence : presences) {
            if (presence.notificationPanelOpen()) {
                return true;
            }
            if (isArticleDetailPresence(presence.viewType(), presence.articleId(), articleId)) {
                return true;
            }
        }
        return false;
    }

    private Boolean tryRedisIsViewingArticleDetail(Long userId, Long articleId) {
        if (!isRedisPresenceEnabled()) {
            return null;
        }
        try {
            return isViewingArticleDetailFromRedis(userId, articleId);
        } catch (Exception ex) {
            if (!isFallbackEnabled()) {
                throw new IllegalStateException("알림 presence Redis 조회에 실패했습니다.", ex);
            }
            log.warn("알림 presence Redis 조회 실패로 메모리 fallback을 사용합니다. userId={}", userId, ex);
            return null;
        }
    }

    private boolean isViewingArticleDetailFromRedis(Long userId, Long articleId) {
        List<RedisPresenceState> presences = notificationPresenceRedisStore.findByUserId(userId);
        if (presences.isEmpty()) {
            return false;
        }
        for (RedisPresenceState presence : presences) {
            if (isArticleDetailPresence(presence.viewType(), presence.articleId(), articleId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isArticleDetailPresence(
        NotificationPresenceViewType viewType,
        Long currentArticleId,
        Long targetArticleId
    ) {
        return targetArticleId != null
            && viewType == NotificationPresenceViewType.ARTICLE_DETAIL
            && targetArticleId.equals(currentArticleId);
    }

    private boolean isRedisPresenceEnabled() {
        return realtimeRedisProperties != null && realtimeRedisProperties.enabled();
    }

    private boolean isFallbackEnabled() {
        return realtimeRedisProperties == null || realtimeRedisProperties.fallbackEnabled();
    }

    private Duration resolvePresenceTtl() {
        if (realtimeRedisProperties == null || realtimeRedisProperties.presenceTtl() == null) {
            return Duration.ofSeconds(45);
        }
        return realtimeRedisProperties.presenceTtl();
    }

    private int resolvePresenceMaxSessions() {
        if (realtimeRedisProperties == null || realtimeRedisProperties.presenceMaxSessions() <= 0) {
            return 8;
        }
        return realtimeRedisProperties.presenceMaxSessions();
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
