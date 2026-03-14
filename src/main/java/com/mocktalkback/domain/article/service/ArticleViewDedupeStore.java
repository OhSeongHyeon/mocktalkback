package com.mocktalkback.domain.article.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleViewDedupeStore {

    private static final String KEY_PREFIX = "article:view:dedupe:v1:";

    private final StringRedisTemplate stringRedisTemplate;

    public boolean markViewed(Long articleId, String viewerKey, Duration ttl) {
        if (articleId == null) {
            throw new IllegalArgumentException("게시글 ID가 비어 있습니다.");
        }
        if (!StringUtils.hasText(viewerKey)) {
            throw new IllegalArgumentException("조회 식별 키가 비어 있습니다.");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("조회 dedupe TTL이 올바르지 않습니다.");
        }
        Boolean stored = stringRedisTemplate.opsForValue().setIfAbsent(key(articleId, viewerKey), "1", ttl);
        return Boolean.TRUE.equals(stored);
    }

    private String key(Long articleId, String viewerKey) {
        return KEY_PREFIX + articleId + ":" + viewerKey.trim();
    }
}
