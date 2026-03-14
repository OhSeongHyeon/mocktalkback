package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.dto.ArticleTrendingItemResponse;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.policy.PublicArticleFeedPolicy;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.article.type.ArticleContentFormat;
import com.mocktalkback.domain.article.type.ArticleTrendingWindow;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.common.policy.AuthorDisplayResolver;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;

@ExtendWith(MockitoExtension.class)
class ArticleTrendingServiceTest {

    @Mock
    private ArticleTrendingStore articleTrendingStore;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ArticleReactionRepository articleReactionRepository;

    // 게시글 반응 전환은 모든 버킷에 동일한 delta를 반영해야 한다.
    @Test
    void recordArticleReactionChanged_applies_transition_delta_to_all_buckets() {
        // Given: 고정 시각의 트렌딩 서비스
        ArticleTrendingService service = new ArticleTrendingService(
            articleTrendingStore,
            articleRepository,
            commentRepository,
            articleReactionRepository,
            new AuthorDisplayResolver(),
            new PublicArticleFeedPolicy(),
            Clock.fixed(Instant.parse("2026-03-12T09:15:30Z"), ZoneId.of("UTC"))
        );

        // When: 반응이 없음에서 좋아요로 바뀌면
        service.recordArticleReactionChanged(10L, (short) 0, (short) 1);

        // Then: 좋아요 가중치가 모든 버킷에 반영되어야 한다.
        verify(articleTrendingStore).incrementScore(eq("trend:article:hour:2026031218"), eq(10L), eq(3.0d), any());
        verify(articleTrendingStore).incrementScore(eq("trend:article:day:20260312"), eq(10L), eq(3.0d), any());
        verify(articleTrendingStore).incrementScore(eq("trend:article:week:202611"), eq(10L), eq(3.0d), any());
    }

    // 공개 인기글 조회는 Redis 순위를 유지한 채 공개 게시글만 응답해야 한다.
    @Test
    void findTrendingPublic_returns_ranked_public_articles() {
        // Given: 공개 게시글 1건과 Redis 랭킹
        ArticleTrendingService service = new ArticleTrendingService(
            articleTrendingStore,
            articleRepository,
            commentRepository,
            articleReactionRepository,
            new AuthorDisplayResolver(),
            new PublicArticleFeedPolicy(),
            Clock.fixed(Instant.parse("2026-03-12T09:15:30Z"), ZoneId.of("UTC"))
        );

        BoardEntity board = BoardEntity.builder()
            .boardName("자유")
            .slug("free")
            .description("테스트")
            .visibility(BoardVisibility.PUBLIC)
            .build();
        ReflectionTestUtils.setField(board, "id", 1L);

        RoleEntity role = RoleEntity.create("USER", 0, "테스트");
        UserEntity user = UserEntity.createLocal(role, "login", "user@test.com", "pw", "user", "display", "handle");
        ReflectionTestUtils.setField(user, "id", 2L);

        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(user)
            .category(null)
            .visibility(ContentVisibility.PUBLIC)
            .title("트렌딩 제목")
            .content("content")
            .contentSource("content")
            .contentFormat(ArticleContentFormat.HTML)
            .notice(false)
            .hit(12L)
            .build();
        ReflectionTestUtils.setField(article, "id", 10L);

        when(articleTrendingStore.findTopArticles("trend:article:day:20260312", 30))
            .thenReturn(List.of(new ArticleTrendingStore.RankedArticle(10L, 18.0d)));
        when(articleRepository.findAllByIdInAndDeletedAtIsNull(List.of(10L))).thenReturn(List.of(article));
        when(commentRepository.countByArticleIds(List.of(10L))).thenReturn(List.of());
        when(articleReactionRepository.countByArticleIds(List.of(10L))).thenReturn(List.of());

        // When: 일간 인기글을 조회하면
        List<ArticleTrendingItemResponse> items = service.findTrendingPublic(ArticleTrendingWindow.DAY, 10);

        // Then: 공개 게시글 순위와 점수를 반환해야 한다.
        assertThat(items).hasSize(1);
        assertThat(items.get(0).articleId()).isEqualTo(10L);
        assertThat(items.get(0).boardSlug()).isEqualTo("free");
        assertThat(items.get(0).trendScore()).isEqualTo(18.0d);
    }

    // 공개 인기글 조회는 공지사항/문의 게시판 글을 제외해야 한다.
    @Test
    void findTrendingPublic_excludes_notice_and_inquiry_boards() {
        // Given: 공지사항 게시판 글 1건과 Redis 랭킹
        ArticleTrendingService service = new ArticleTrendingService(
            articleTrendingStore,
            articleRepository,
            commentRepository,
            articleReactionRepository,
            new AuthorDisplayResolver(),
            new PublicArticleFeedPolicy(),
            Clock.fixed(Instant.parse("2026-03-12T09:15:30Z"), ZoneId.of("UTC"))
        );

        BoardEntity board = BoardEntity.builder()
            .boardName("공지사항")
            .slug("notice")
            .description("테스트")
            .visibility(BoardVisibility.PUBLIC)
            .build();
        ReflectionTestUtils.setField(board, "id", 1L);

        RoleEntity role = RoleEntity.create("USER", 0, "테스트");
        UserEntity user = UserEntity.createLocal(role, "login", "user@test.com", "pw", "user", "display", "handle");
        ReflectionTestUtils.setField(user, "id", 2L);

        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(user)
            .category(null)
            .visibility(ContentVisibility.PUBLIC)
            .title("공지 제목")
            .content("content")
            .contentSource("content")
            .contentFormat(ArticleContentFormat.HTML)
            .notice(false)
            .hit(12L)
            .build();
        ReflectionTestUtils.setField(article, "id", 10L);

        when(articleTrendingStore.findTopArticles("trend:article:day:20260312", 30))
            .thenReturn(List.of(new ArticleTrendingStore.RankedArticle(10L, 18.0d)));
        when(articleRepository.findAllByIdInAndDeletedAtIsNull(List.of(10L))).thenReturn(List.of(article));

        // When: 일간 인기글을 조회하면
        List<ArticleTrendingItemResponse> items = service.findTrendingPublic(ArticleTrendingWindow.DAY, 10);

        // Then: 공지사항 게시판 글은 결과에서 제외되어야 한다.
        assertThat(items).isEmpty();
    }
}
