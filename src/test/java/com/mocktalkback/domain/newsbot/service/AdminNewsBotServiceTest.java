package com.mocktalkback.domain.newsbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.newsbot.config.NewsBotProperties;
import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobUpsertRequest;
import com.mocktalkback.domain.newsbot.repository.NewsCollectionJobRepository;
import com.mocktalkback.domain.newsbot.type.NewsSourceType;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class AdminNewsBotServiceTest {

    @Mock
    private NewsCollectionJobRepository newsCollectionJobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NewsBotSourceFetchService newsBotSourceFetchService;

    @Mock
    private NewsBotJobExecutor newsBotJobExecutor;

    @Mock
    private NewsBotProperties newsBotProperties;

    // 뉴스봇 잡 생성은 system author와 기본 timezone을 사용해야 한다.
    @Test
    void createJob_usesSystemAuthorAndDefaultTimezone() {
        // Given: 관리자와 news_bot 계정이 준비되어 있고 timezone 입력은 비어 있다.
        Instant now = Instant.parse("2026-03-15T10:00:00Z");
        AdminNewsBotService service = new AdminNewsBotService(
            newsCollectionJobRepository,
            userRepository,
            currentUserService,
            newsBotSourceFetchService,
            newsBotJobExecutor,
            newsBotProperties,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        UserEntity admin = createUser(10L, "admin", "Admin");
        UserEntity newsBot = createUser(20L, "news_bot", "뉴스봇");
        AdminNewsBotJobUpsertRequest request = new AdminNewsBotJobUpsertRequest(
            "백엔드 새소식",
            NewsSourceType.DEV_TO,
            Map.of("tag", "backend"),
            "backend-news",
            "백엔드 새소식",
            "DEV",
            60,
            20,
            true,
            true,
            null
        );

        when(currentUserService.getUserId()).thenReturn(10L);
        when(userRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(admin));
        when(userRepository.findByLoginId("news_bot")).thenReturn(Optional.of(newsBot));
        when(newsCollectionJobRepository.existsByJobName("백엔드 새소식")).thenReturn(false);
        when(newsBotProperties.getDefaultTimezone()).thenReturn("Asia/Seoul");
        when(newsBotSourceFetchService.serialize(request.sourceConfig())).thenReturn("{\"tag\":\"backend\"}");
        when(newsBotSourceFetchService.deserialize("{\"tag\":\"backend\"}")).thenReturn(Map.of("tag", "backend"));
        when(newsCollectionJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: 관리자가 새 뉴스봇 잡을 생성하면
        var response = service.createJob(request);

        // Then: 작성자는 news_bot 이어야 하고, timezone 은 기본값으로 채워져야 한다.
        assertThat(response.authorUserId()).isEqualTo(20L);
        assertThat(response.authorDisplayName()).isEqualTo("뉴스봇");
        assertThat(response.timezone()).isEqualTo("Asia/Seoul");
        assertThat(response.nextRunAt()).isEqualTo(now);
        assertThat(response.enabled()).isTrue();
    }

    private UserEntity createUser(Long userId, String loginId, String displayName) {
        RoleEntity role = RoleEntity.create("WRITER", 3, "작성");
        ReflectionTestUtils.setField(role, "id", 1L);
        UserEntity user = UserEntity.createLocal(
            role,
            loginId,
            loginId + "@dummy.du",
            "encoded-password",
            displayName,
            displayName,
            loginId
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
