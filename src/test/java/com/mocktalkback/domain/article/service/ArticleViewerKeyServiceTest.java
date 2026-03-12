package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleViewerKeyServiceTest {

    // 로그인 사용자는 user 접두어 기반 키를 사용해야 한다.
    @Test
    void resolve_returns_user_key_for_authenticated_user() {
        // Given: 조회 식별 키 생성기
        ArticleViewerKeyService service = new ArticleViewerKeyService();

        // When: 로그인 사용자 키를 생성하면
        String viewerKey = service.resolve(12L, "127.0.0.1", "MockBrowser/1.0");

        // Then: user 키를 반환해야 한다.
        assertThat(viewerKey).isEqualTo("user:12");
    }

    // 비로그인 사용자는 같은 IP/User-Agent 조합에서 동일한 익명 키를 반환해야 한다.
    @Test
    void resolve_returns_stable_anon_key_for_same_metadata() {
        // Given: 조회 식별 키 생성기와 고정 비밀키
        ArticleViewerKeyService service = new ArticleViewerKeyService();
        ReflectionTestUtils.setField(service, "anonHashSecret", "test-secret");

        // When: 같은 메타데이터로 두 번 익명 키를 생성하면
        String first = service.resolve(null, "203.0.113.10", "MockBrowser/1.0");
        String second = service.resolve(null, "203.0.113.10", "MockBrowser/1.0");

        // Then: 같은 익명 키를 반환해야 한다.
        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("anon:");
    }

    // 비로그인 사용자는 IP가 달라지면 다른 익명 키를 반환해야 한다.
    @Test
    void resolve_returns_different_anon_key_when_ip_changes() {
        // Given: 조회 식별 키 생성기와 고정 비밀키
        ArticleViewerKeyService service = new ArticleViewerKeyService();
        ReflectionTestUtils.setField(service, "anonHashSecret", "test-secret");

        // When: 다른 IP로 익명 키를 생성하면
        String first = service.resolve(null, "203.0.113.10", "MockBrowser/1.0");
        String second = service.resolve(null, "203.0.113.11", "MockBrowser/1.0");

        // Then: 서로 다른 익명 키를 반환해야 한다.
        assertThat(first).isNotEqualTo(second);
    }
}
