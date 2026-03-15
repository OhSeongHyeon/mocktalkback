package com.mocktalkback.domain.newsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/* 환경변수 제어
NEWS_BOT_ENABLED: 기능 전체 on/off
NEWS_BOT_DISPATCHER_INTERVAL_MS: due job 스캔 주기
NEWS_BOT_CONNECT_TIMEOUT_MS: 외부 호출 연결 timeout
NEWS_BOT_READ_TIMEOUT_MS: 외부 호출 응답 읽기 timeout
NEWS_BOT_USER_AGENT: 외부 호출 User-Agent
NEWS_BOT_DEFAULT_TIMEZONE: 잡 timezone 기본값
NEWS_BOT_RUN_LOCK_TIMEOUT_MINUTES: 비정상 종료된 RUNNING 상태 재선점 허용 시간
 */
@ConfigurationProperties(prefix = "app.news-bot")
public class NewsBotProperties {

    private boolean enabled = true;
    private long dispatcherIntervalMs = 60000L;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private String userAgent = "mocktalk-news-bot/1.0";
    private String defaultTimezone = "Asia/Seoul";
    private int runLockTimeoutMinutes = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDispatcherIntervalMs() {
        return dispatcherIntervalMs;
    }

    public void setDispatcherIntervalMs(long dispatcherIntervalMs) {
        this.dispatcherIntervalMs = dispatcherIntervalMs;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public int getRunLockTimeoutMinutes() {
        return runLockTimeoutMinutes;
    }

    public void setRunLockTimeoutMinutes(int runLockTimeoutMinutes) {
        this.runLockTimeoutMinutes = runLockTimeoutMinutes;
    }
}
