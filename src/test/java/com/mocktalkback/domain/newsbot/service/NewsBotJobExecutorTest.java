package com.mocktalkback.domain.newsbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobRunResponse;
import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;
import com.mocktalkback.domain.newsbot.repository.NewsCollectionJobRepository;
import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;
import com.mocktalkback.domain.newsbot.type.NewsSourceType;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

@ExtendWith(MockitoExtension.class)
class NewsBotJobExecutorTest {

    @Mock
    private NewsCollectionJobRepository newsCollectionJobRepository;

    @Mock
    private NewsBotSourceFetchService newsBotSourceFetchService;

    @Mock
    private NewsBotJobExecutionClaimService newsBotJobExecutionClaimService;

    @Mock
    private NewsBotJobExecutionPersistenceService newsBotJobExecutionPersistenceService;

    // 스케줄 실행 선점에 실패하면 외부 호출 없이 바로 건너뛰어야 한다.
    @Test
    void runScheduled_skipsWhenClaimFails() {
        // Given: 다른 실행 주체가 이미 잡을 선점했다.
        Instant now = Instant.parse("2026-03-15T10:00:00Z");
        NewsBotJobExecutor executor = new NewsBotJobExecutor(
            newsCollectionJobRepository,
            newsBotSourceFetchService,
            newsBotJobExecutionClaimService,
            newsBotJobExecutionPersistenceService,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        when(newsBotJobExecutionClaimService.claimScheduledRun(1L, now)).thenReturn(false);

        // When: 스케줄러가 해당 잡을 실행하려고 하면
        AdminNewsBotJobRunResponse response = executor.runScheduled(1L);

        // Then: 실제 수집은 시작되지 않아야 한다.
        assertThat(response).isNull();
        verify(newsBotJobExecutionClaimService).claimScheduledRun(1L, now);
        verifyNoInteractions(newsCollectionJobRepository, newsBotSourceFetchService, newsBotJobExecutionPersistenceService);
    }

    // 수동 실행은 선점 이후 외부 조회와 적재 처리로 이어져야 한다.
    @Test
    void runNow_fetchesItemsAndDelegatesPersistence() {
        // Given: 수동 실행 선점이 성공했고 외부 소스에서 항목 1건을 가져온다.
        Instant now = Instant.parse("2026-03-15T10:00:00Z");
        NewsBotJobExecutor executor = new NewsBotJobExecutor(
            newsCollectionJobRepository,
            newsBotSourceFetchService,
            newsBotJobExecutionClaimService,
            newsBotJobExecutionPersistenceService,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        NewsCollectionJobEntity job = createJob(now);
        NewsBotSourceItem item = new NewsBotSourceItem(
            "dev-1",
            "Spring Boot Release",
            "https://dev.to/example",
            "새 릴리스 요약",
            "DEV Community",
            "Ben",
            now,
            now
        );
        AdminNewsBotJobRunResponse expected = new AdminNewsBotJobRunResponse(
            1L,
            now,
            1,
            1,
            0,
            0,
            0,
            NewsJobExecutionStatus.SUCCESS,
            null
        );

        when(newsCollectionJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(newsBotSourceFetchService.fetchItems(job)).thenReturn(List.of(item));
        when(newsBotJobExecutionPersistenceService.processFetchedItems(1L, now, List.of(item))).thenReturn(expected);

        // When: 운영자가 지금 실행을 누르면
        AdminNewsBotJobRunResponse response = executor.runNow(1L);

        // Then: 선점 후 적재 서비스로 위임되어야 한다.
        assertThat(response).isEqualTo(expected);
        verify(newsBotJobExecutionClaimService).claimManualRun(1L, now);
        verify(newsBotSourceFetchService).fetchItems(job);
        verify(newsBotJobExecutionPersistenceService).processFetchedItems(1L, now, List.of(item));
    }

    private NewsCollectionJobEntity createJob(Instant now) {
        UserEntity admin = createUser(10L, "admin", "Admin");
        UserEntity newsBot = createUser(20L, "news_bot", "뉴스봇");
        NewsCollectionJobEntity job = NewsCollectionJobEntity.create(
            "백엔드 새소식",
            NewsSourceType.DEV_TO,
            "{\"tag\":\"backend\"}",
            "backend-news",
            "백엔드 새소식",
            "DEV",
            newsBot,
            admin,
            60,
            20,
            true,
            true,
            "Asia/Seoul",
            now
        );
        ReflectionTestUtils.setField(job, "id", 1L);
        return job;
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
