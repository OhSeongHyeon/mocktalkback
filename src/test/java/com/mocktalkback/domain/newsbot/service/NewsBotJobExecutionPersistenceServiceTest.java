package com.mocktalkback.domain.newsbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardArticleWritePolicy;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobRunResponse;
import com.mocktalkback.domain.newsbot.entity.NewsCollectionJobEntity;
import com.mocktalkback.domain.newsbot.repository.NewsCollectedItemRepository;
import com.mocktalkback.domain.newsbot.repository.NewsCollectionJobRepository;
import com.mocktalkback.domain.newsbot.type.NewsJobExecutionStatus;
import com.mocktalkback.domain.newsbot.type.NewsSourceType;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

@ExtendWith(MockitoExtension.class)
class NewsBotJobExecutionPersistenceServiceTest {

    @Mock
    private NewsCollectionJobRepository newsCollectionJobRepository;

    @Mock
    private NewsCollectedItemRepository newsCollectedItemRepository;

    @Mock
    private NewsBotBoardProvisionService newsBotBoardProvisionService;

    @Mock
    private NewsBotArticlePublishService newsBotArticlePublishService;

    // 첫 수집 항목은 내부 게시글을 생성하고 SUCCESS 로 집계해야 한다.
    @Test
    void processFetchedItems_createsArticleForNewCollectedItem() {
        // Given: 새 수집 항목 1건이 있고 기존 dedupe 기록은 없다.
        Instant now = Instant.parse("2026-03-15T10:00:00Z");
        NewsBotJobExecutionPersistenceService service = new NewsBotJobExecutionPersistenceService(
            newsCollectionJobRepository,
            newsCollectedItemRepository,
            newsBotBoardProvisionService,
            newsBotArticlePublishService,
            new NewsBotPayloadHasher(),
            Clock.fixed(now, ZoneOffset.UTC)
        );
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
        BoardEntity board = BoardEntity.builder()
            .boardName("백엔드 새소식")
            .slug("backend-news")
            .description("설명")
            .visibility(BoardVisibility.PUBLIC)
            .articleWritePolicy(BoardArticleWritePolicy.OWNER)
            .build();
        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(newsBot)
            .visibility(com.mocktalkback.domain.role.type.ContentVisibility.PUBLIC)
            .title("Spring Boot Release")
            .content("content")
            .contentSource("content")
            .contentFormat(com.mocktalkback.domain.article.type.ArticleContentFormat.MARKDOWN)
            .hit(0L)
            .notice(false)
            .build();
        ReflectionTestUtils.setField(article, "id", 100L);
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

        when(newsCollectionJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(newsBotBoardProvisionService.ensureBoard(job)).thenReturn(board);
        when(newsBotBoardProvisionService.ensureCategory(board, job)).thenReturn(null);
        when(newsCollectedItemRepository.findByNewsJob_IdAndExternalItemKey(1L, "dev-1")).thenReturn(Optional.empty());
        when(newsCollectedItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(newsBotArticlePublishService.createArticle(job, board, newsBot, null, item)).thenReturn(article);

        // When: 적재 처리 서비스가 새 수집 항목을 저장하면
        AdminNewsBotJobRunResponse response = service.processFetchedItems(1L, now, List.of(item));

        // Then: 새 게시글 1건이 생성되어야 한다.
        assertThat(response.status()).isEqualTo(NewsJobExecutionStatus.SUCCESS);
        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isZero();
        assertThat(response.skippedCount()).isZero();
        assertThat(response.failedCount()).isZero();
        verify(newsBotArticlePublishService).createArticle(job, board, newsBot, null, item);
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
