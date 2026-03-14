package com.mocktalkback.domain.article.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mocktalkback.global.auth.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleViewService {

    private final ArticleHitService articleHitService;
    private final ArticleViewDedupeStore articleViewDedupeStore;
    private final ArticleViewerKeyService articleViewerKeyService;
    private final ArticleTrendingService articleTrendingService;
    private final CurrentUserService currentUserService;

    @Value("${app.article.view.dedupe-ttl-seconds:86400}")
    private long dedupeTtlSeconds;

    public long increaseHitIfEligible(Long articleId, long currentHit, String clientIp, String userAgent) {
        Long userId = currentUserService.getOptionalUserId().orElse(null);
        String viewerKey = articleViewerKeyService.resolve(userId, clientIp, userAgent);

        try {
            boolean firstView = articleViewDedupeStore.markViewed(articleId, viewerKey, resolveDedupeTtl());
            if (!firstView) {
                return currentHit;
            }
        } catch (Exception ex) {
            log.warn("게시글 조회 dedupe 처리에 실패해 조회수 증가를 생략합니다. articleId={}", articleId, ex);
            return currentHit;
        }

        long updatedHit = articleHitService.increaseAndGet(articleId);
        articleTrendingService.recordView(articleId);
        return updatedHit;
    }

    private Duration resolveDedupeTtl() {
        return Duration.ofSeconds(Math.max(1L, dedupeTtlSeconds));
    }
}
