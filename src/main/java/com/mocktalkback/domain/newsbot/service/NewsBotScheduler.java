package com.mocktalkback.domain.newsbot.service;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mocktalkback.domain.newsbot.config.NewsBotProperties;

import lombok.RequiredArgsConstructor;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class NewsBotScheduler {

    private final NewsBotProperties newsBotProperties;
    private final NewsBotDispatchService newsBotDispatchService;

    @Scheduled(fixedDelayString = "${app.news-bot.dispatcher-interval-ms:60000}")
    public void dispatchDueJobs() {
        if (!newsBotProperties.isEnabled()) {
            return;
        }
        newsBotDispatchService.dispatchDueJobs();
    }
}
