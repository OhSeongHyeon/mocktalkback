package com.mocktalkback.domain.file.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file.delete-retry")
public class StorageDeleteRetryProperties {

    private boolean enabled = true;
    private long intervalMs = 30_000L;
    private int batchSize = 200;
    private long initialDelaySeconds = 30L;
    private long maxDelaySeconds = 3_600L;
    private int maxAttempts = 10;
    private long dlqRetentionSeconds = 2_592_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(long initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }

    public long getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(long maxDelaySeconds) {
        this.maxDelaySeconds = maxDelaySeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getDlqRetentionSeconds() {
        return dlqRetentionSeconds;
    }

    public void setDlqRetentionSeconds(long dlqRetentionSeconds) {
        this.dlqRetentionSeconds = dlqRetentionSeconds;
    }

    public int resolveBatchSize() {
        return Math.max(1, batchSize);
    }

    public long resolveInitialDelaySeconds() {
        return Math.max(1L, initialDelaySeconds);
    }

    public long resolveMaxDelaySeconds() {
        return Math.max(resolveInitialDelaySeconds(), maxDelaySeconds);
    }

    public int resolveMaxAttempts() {
        return Math.max(1, maxAttempts);
    }

    public long resolveDlqRetentionSeconds() {
        return Math.max(0L, dlqRetentionSeconds);
    }

    public long resolveRetryDelaySeconds(int attempt) {
        int safeAttempt = Math.max(1, attempt);
        long delay = resolveInitialDelaySeconds();
        long maxDelay = resolveMaxDelaySeconds();
        for (int i = 1; i < safeAttempt; i++) {
            if (delay >= maxDelay) {
                return maxDelay;
            }
            long doubled = delay * 2L;
            if (doubled < 0L) {
                return maxDelay;
            }
            delay = Math.min(doubled, maxDelay);
        }
        return Math.min(delay, maxDelay);
    }
}

