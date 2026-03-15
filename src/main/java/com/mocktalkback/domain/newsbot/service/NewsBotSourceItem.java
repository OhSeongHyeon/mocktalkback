package com.mocktalkback.domain.newsbot.service;

import java.time.Instant;

public record NewsBotSourceItem(
    String externalItemKey,
    String title,
    String externalUrl,
    String summary,
    String sourceLabel,
    String authorName,
    Instant publishedAt,
    Instant sourceUpdatedAt
) {
}
