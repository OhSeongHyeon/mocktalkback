package com.mocktalkback.domain.content.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/* 환경변수 제어
CONTENT_MARKET_ENABLED: 기능 전체 on/off
CONTENT_MARKET_STARTUP_COLLECT_ENABLED: 서버 시작 시 데이터가 비어 있으면 1회 수집
CONTENT_MARKET_COLLECT_CRON: 정기 수집 cron
CONTENT_MARKET_TIMEZONE: 수집 시간대
CONTENT_MARKET_BASE_URL: 외부 시세 API 주소
CONTENT_MARKET_CHART_PATH: 시세 조회 path
CONTENT_MARKET_INTERVAL: 외부 API 조회 간격
CONTENT_MARKET_RANGE: 외부 API 조회 범위
CONTENT_MARKET_USER_AGENT: 외부 호출 User-Agent
CONTENT_MARKET_CONNECT_TIMEOUT_MS: 연결 timeout
CONTENT_MARKET_READ_TIMEOUT_MS: 응답 읽기 timeout
CONTENT_MARKET_SERIES_CACHE_ENABLED: 장기 시계열 Redis 캐시 on/off
CONTENT_MARKET_SERIES_CACHE_TTL_SECONDS: 장기 시계열 Redis 캐시 TTL
 */
@ConfigurationProperties(prefix = "app.content.market")
public class ContentMarketProperties {

    private boolean enabled = true;
    private boolean startupCollectEnabled = true;
    private String collectCron = "0 5 3 * * *";
    private String timezone = "Asia/Seoul";
    private String baseUrl = "https://query1.finance.yahoo.com";
    private String chartPath = "/v8/finance/chart/{symbol}";
    private String interval = "1d";
    private String range = "5d";
    private String userAgent = "mocktalkback/0.1";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private boolean seriesCacheEnabled = true;
    private long seriesCacheTtlSeconds = 86400L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStartupCollectEnabled() {
        return startupCollectEnabled;
    }

    public void setStartupCollectEnabled(boolean startupCollectEnabled) {
        this.startupCollectEnabled = startupCollectEnabled;
    }

    public String getCollectCron() {
        return collectCron;
    }

    public void setCollectCron(String collectCron) {
        this.collectCron = collectCron;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChartPath() {
        return chartPath;
    }

    public void setChartPath(String chartPath) {
        this.chartPath = chartPath;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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

    public boolean isSeriesCacheEnabled() {
        return seriesCacheEnabled;
    }

    public void setSeriesCacheEnabled(boolean seriesCacheEnabled) {
        this.seriesCacheEnabled = seriesCacheEnabled;
    }

    public long getSeriesCacheTtlSeconds() {
        return seriesCacheTtlSeconds;
    }

    public void setSeriesCacheTtlSeconds(long seriesCacheTtlSeconds) {
        this.seriesCacheTtlSeconds = seriesCacheTtlSeconds;
    }
}
