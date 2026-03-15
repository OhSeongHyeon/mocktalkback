package com.mocktalkback.domain.content.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mocktalkback.domain.content.config.ContentMarketProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class ContentMarketScheduler {

    private final ContentMarketProperties properties;
    private final MarketSnapshotCollectorService marketSnapshotCollectorService;

    @Scheduled(cron = "${app.content.market.collect-cron:0 5 3 * * *}", zone = "${app.content.market.timezone:Asia/Seoul}")
    public void collectDailySnapshots() {
        if (!properties.isEnabled()) {
            return;
        }
        marketSnapshotCollectorService.collectLatestSnapshots();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void collectOnStartupWhenEmpty() {
        if (!properties.isEnabled() || !properties.isStartupCollectEnabled()) {
            return;
        }
        if (marketSnapshotCollectorService.hasAnySnapshot()) {
            return;
        }

        log.info("초기 시세 데이터가 없어 앱 시작 시 1회 수집을 시도합니다.");
        marketSnapshotCollectorService.collectLatestSnapshots();
    }
}
