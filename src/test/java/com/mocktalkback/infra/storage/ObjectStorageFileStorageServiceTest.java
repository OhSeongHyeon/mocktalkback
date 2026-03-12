package com.mocktalkback.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ObjectStorageFileStorageServiceTest {

    // 공개 조회 URL은 publicBaseUrl이 있으면 해당 URL을 그대로 사용해야 한다.
    @Test
    void resolveViewUrl_uses_public_base_url_when_configured() {
        // given: publicBaseUrl이 설정된 오브젝트 스토리지 서비스가 있다.
        ObjectStorageFileStorageService service = new ObjectStorageFileStorageService(createProperties());

        // when: 일반 조회 URL을 해석하면
        String location = service.resolveViewUrl("uploads/article_content_image/7/2026/03/12/file.png");

        // then: publicBaseUrl 기반 공개 URL을 반환한다.
        assertThat(location).isEqualTo("https://cdn.mocktalk.test/uploads/article_content_image/7/2026/03/12/file.png");
    }

    // 보호 파일 조회 URL은 publicBaseUrl을 무시하고 보호용 presigned URL을 발급해야 한다.
    @Test
    void resolveProtectedViewUrl_ignores_public_base_url_and_uses_protected_ttl() {
        // given: publicBaseUrl과 보호 파일 전용 TTL이 설정된 오브젝트 스토리지 서비스가 있다.
        ObjectStorageFileStorageService service = new ObjectStorageFileStorageService(createProperties());

        // when: 보호 파일 조회 URL을 해석하면
        String location = service.resolveProtectedViewUrl("uploads/article_content_image/7/2026/03/12/file.png");

        // then: 공개 CDN URL이 아니라 /storage 프록시 기반 presigned URL을 반환하고 보호 TTL을 사용한다.
        assertThat(location).startsWith("/storage/mocktalk/uploads/article_content_image/7/2026/03/12/file.png");
        assertThat(location).contains("X-Amz-Expires=120");
        assertThat(location).doesNotStartWith("https://cdn.mocktalk.test/");
    }

    // 보호 파일 조회 URL은 ticket 남은 TTL보다 길게 발급되면 안 된다.
    @Test
    void resolveProtectedViewUrl_clamps_expire_seconds_to_remaining_ticket_ttl() {
        // given: 보호 파일 TTL보다 짧은 ticket 남은 시간이 있다.
        ObjectStorageFileStorageService service = new ObjectStorageFileStorageService(createProperties());

        // when: 남은 TTL을 지정해 보호 파일 조회 URL을 해석하면
        String location = service.resolveProtectedViewUrl(
            "uploads/article_content_image/7/2026/03/12/file.png",
            Duration.ofSeconds(45L)
        );

        // then: presigned URL 만료시간도 ticket 남은 시간에 맞춰 줄어든다.
        assertThat(location).contains("X-Amz-Expires=45");
    }

    private ObjectStorageProperties createProperties() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setEndpoint("https://origin.mocktalk.test");
        properties.setPresignEndpoint("https://presign.mocktalk.test");
        properties.setRegion("ap-seoul-1");
        properties.setBucket("mocktalk");
        properties.setAccessKey("test-access");
        properties.setSecretKey("test-secret");
        properties.setPublicBaseUrl("https://cdn.mocktalk.test");
        properties.setPresignExpireSeconds(300L);
        properties.setProtectedViewExpireSeconds(120L);
        return properties;
    }
}
