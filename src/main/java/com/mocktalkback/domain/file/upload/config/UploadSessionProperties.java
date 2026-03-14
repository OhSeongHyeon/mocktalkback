package com.mocktalkback.domain.file.upload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public class UploadSessionProperties {

    private long sessionTtlSeconds = 600L;
    private long orphanCleanupGraceSeconds = 120L;

    public long getSessionTtlSeconds() {
        return sessionTtlSeconds;
    }

    public void setSessionTtlSeconds(long sessionTtlSeconds) {
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    public long getOrphanCleanupGraceSeconds() {
        return orphanCleanupGraceSeconds;
    }

    public void setOrphanCleanupGraceSeconds(long orphanCleanupGraceSeconds) {
        this.orphanCleanupGraceSeconds = orphanCleanupGraceSeconds;
    }
}
