package com.mocktalkback.domain.realtime.config;

import java.time.Duration;

public record RealtimeRedisProperties(
    boolean enabled,
    boolean fallbackEnabled,
    String notificationChannel,
    String boardChannel,
    Duration presenceTtl,
    int presenceMaxSessions
) {
    private static final String DEFAULT_NOTIFICATION_CHANNEL = "realtime:notification:events";
    private static final String DEFAULT_BOARD_CHANNEL = "realtime:board:events";
    private static final Duration DEFAULT_PRESENCE_TTL = Duration.ofSeconds(45);
    private static final int DEFAULT_PRESENCE_MAX_SESSIONS = 8;

    public static RealtimeRedisProperties defaults() {
        return new RealtimeRedisProperties(
            false,
            true,
            DEFAULT_NOTIFICATION_CHANNEL,
            DEFAULT_BOARD_CHANNEL,
            DEFAULT_PRESENCE_TTL,
            DEFAULT_PRESENCE_MAX_SESSIONS
        );
    }
}
