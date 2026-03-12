package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class ArticleViewServiceTest {

    @Mock
    private ArticleHitService articleHitService;

    @Mock
    private ArticleViewDedupeStore articleViewDedupeStore;

    @Mock
    private ArticleViewerKeyService articleViewerKeyService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ArticleViewService articleViewService;

    // 조회 dedupe를 통과한 첫 조회는 원자 조회수 증가를 수행해야 한다.
    @Test
    void increaseHitIfEligible_increases_hit_when_first_view() {
        // Given: 로그인 사용자의 첫 조회
        ReflectionTestUtils.setField(articleViewService, "dedupeTtlSeconds", 86400L);
        when(currentUserService.getOptionalUserId()).thenReturn(Optional.of(2L));
        when(articleViewerKeyService.resolve(2L, "127.0.0.1", "MockBrowser/1.0")).thenReturn("user:2");
        when(articleViewDedupeStore.markViewed(eq(10L), eq("user:2"), any(Duration.class))).thenReturn(true);
        when(articleHitService.increaseAndGet(10L)).thenReturn(8L);

        // When: 조회수를 반영하면
        long hit = articleViewService.increaseHitIfEligible(10L, 7L, "127.0.0.1", "MockBrowser/1.0");

        // Then: 원자 조회수 증가 결과를 반환해야 한다.
        assertThat(hit).isEqualTo(8L);
        verify(articleHitService).increaseAndGet(10L);
    }

    // 중복 조회는 조회수 증가 없이 현재 hit 값을 그대로 반환해야 한다.
    @Test
    void increaseHitIfEligible_returns_current_hit_when_duplicate_view() {
        // Given: 이미 기록된 조회
        ReflectionTestUtils.setField(articleViewService, "dedupeTtlSeconds", 86400L);
        when(currentUserService.getOptionalUserId()).thenReturn(Optional.of(2L));
        when(articleViewerKeyService.resolve(2L, "127.0.0.1", "MockBrowser/1.0")).thenReturn("user:2");
        when(articleViewDedupeStore.markViewed(eq(10L), eq("user:2"), any(Duration.class))).thenReturn(false);

        // When: 조회수를 반영하면
        long hit = articleViewService.increaseHitIfEligible(10L, 7L, "127.0.0.1", "MockBrowser/1.0");

        // Then: 현재 조회수만 반환해야 한다.
        assertThat(hit).isEqualTo(7L);
        verify(articleHitService, never()).increaseAndGet(10L);
    }

    // Redis dedupe 처리 실패 시 조회수 증가는 생략하고 현재 hit 값을 유지해야 한다.
    @Test
    void increaseHitIfEligible_returns_current_hit_when_dedupe_store_fails() {
        // Given: Redis dedupe 저장 실패
        ReflectionTestUtils.setField(articleViewService, "dedupeTtlSeconds", 86400L);
        when(currentUserService.getOptionalUserId()).thenReturn(Optional.empty());
        when(articleViewerKeyService.resolve(null, "127.0.0.1", "MockBrowser/1.0"))
            .thenReturn("anon:test");
        when(articleViewDedupeStore.markViewed(eq(10L), eq("anon:test"), any(Duration.class)))
            .thenThrow(new IllegalStateException("redis down"));

        // When: 조회수를 반영하면
        long hit = articleViewService.increaseHitIfEligible(10L, 7L, "127.0.0.1", "MockBrowser/1.0");

        // Then: 현재 조회수를 유지해야 한다.
        assertThat(hit).isEqualTo(7L);
        verify(articleHitService, never()).increaseAndGet(10L);
    }
}
