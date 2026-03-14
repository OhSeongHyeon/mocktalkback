package com.mocktalkback.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.article.dto.ArticleRecommendedItemResponse;
import com.mocktalkback.domain.article.dto.ArticleTrendingItemResponse;
import com.mocktalkback.domain.article.entity.ArticleBookmarkEntity;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleReactionEntity;
import com.mocktalkback.domain.article.policy.PublicArticleFeedPolicy;
import com.mocktalkback.domain.article.repository.ArticleBookmarkRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.common.policy.AuthorDisplayResolver;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class ArticleRecommendationServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleBookmarkRepository articleBookmarkRepository;

    @Mock
    private ArticleReactionRepository articleReactionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ArticleTrendingService articleTrendingService;

    @Mock
    private CurrentUserService currentUserService;

    // 로그인 사용자는 북마크 기반 개인화 추천을 받아야 한다.
    @Test
    void findRecommendedPublic_returns_personalized_items_for_authenticated_user() {
        // Given: 북마크한 게시판과 같은 공개 글 후보가 있다.
        ArticleRecommendationService service = createService();
        BoardEntity freeBoard = createBoard(1L, "자유게시판", "free");
        ArticleCategoryEntity javaCategory = createCategory(11L, freeBoard, "java");
        UserEntity currentUser = createUser(9L, "current-user");
        UserEntity author = createUser(2L, "author");
        ArticleEntity bookmarkedArticle = createArticle(100L, freeBoard, author, javaCategory, "북마크 글", Instant.parse("2026-03-11T00:00:00Z"));
        ArticleEntity candidateArticle = createArticle(101L, freeBoard, author, javaCategory, "추천 후보", Instant.parse("2026-03-13T00:00:00Z"));
        ArticleEntity otherArticle = createArticle(102L, createBoard(2L, "취미게시판", "hobby"), author, null, "무관한 글", Instant.parse("2026-03-13T00:00:00Z"));
        ArticleBookmarkEntity bookmark = createBookmark(1L, currentUser, bookmarkedArticle);

        when(currentUserService.getOptionalUserId()).thenReturn(Optional.of(9L));
        when(articleRepository.findByBoardVisibilityAndBoardDeletedAtIsNullAndBoardSlugNotInAndVisibilityAndNoticeFalseAndDeletedAtIsNull(
            BoardVisibility.PUBLIC,
            new PublicArticleFeedPolicy().excludedBoardSlugs(),
            ContentVisibility.PUBLIC,
            PageRequest.of(0, 60)
        )).thenReturn(new SliceImpl<>(List.of(candidateArticle, otherArticle), PageRequest.of(0, 60), false));
        when(articleTrendingService.findTrendingPublic(com.mocktalkback.domain.article.type.ArticleTrendingWindow.DAY, 3)).thenReturn(List.of());
        when(articleBookmarkRepository.findTop20ByUserIdOrderByCreatedAtDescIdDesc(9L)).thenReturn(List.of(bookmark));
        when(articleReactionRepository.findTop20ByUserIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of());
        when(commentRepository.findTop20ByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(9L)).thenReturn(List.of());
        when(commentRepository.countByArticleIds(List.of(101L, 102L))).thenReturn(List.of());
        when(articleReactionRepository.countByArticleIds(List.of(101L, 102L))).thenReturn(List.of());

        // When: 추천 글을 조회하면
        List<ArticleRecommendedItemResponse> items = service.findRecommendedPublic(1);

        // Then: 북마크와 같은 게시판/카테고리의 글이 개인화 추천되어야 한다.
        assertThat(items).hasSize(1);
        assertThat(items.get(0).articleId()).isEqualTo(101L);
        assertThat(items.get(0).personalized()).isTrue();
        assertThat(items.get(0).recommendationReason()).isEqualTo("북마크한 글과 비슷한 게시판 기반");
    }

    // 비로그인 사용자는 트렌딩 기반 fallback 추천을 받아야 한다.
    @Test
    void findRecommendedPublic_returns_fallback_items_for_anonymous_user() {
        // Given: 비로그인 상태와 트렌딩 글 1건이 있다.
        ArticleRecommendationService service = createService();
        BoardEntity freeBoard = createBoard(1L, "자유게시판", "free");
        UserEntity author = createUser(2L, "author");
        ArticleEntity candidateArticle = createArticle(201L, freeBoard, author, null, "인기 글", Instant.parse("2026-03-13T00:00:00Z"));

        when(currentUserService.getOptionalUserId()).thenReturn(Optional.empty());
        when(articleRepository.findByBoardVisibilityAndBoardDeletedAtIsNullAndBoardSlugNotInAndVisibilityAndNoticeFalseAndDeletedAtIsNull(
            BoardVisibility.PUBLIC,
            new PublicArticleFeedPolicy().excludedBoardSlugs(),
            ContentVisibility.PUBLIC,
            PageRequest.of(0, 60)
        )).thenReturn(new SliceImpl<>(List.of(candidateArticle), PageRequest.of(0, 60), false));
        when(articleTrendingService.findTrendingPublic(com.mocktalkback.domain.article.type.ArticleTrendingWindow.DAY, 3)).thenReturn(List.of(
            new ArticleTrendingItemResponse(201L, 1L, "free", 2L, "author", "인기 글", 10L, 0L, 0L, 0L, 18.0d, Instant.parse("2026-03-13T00:00:00Z"))
        ));
        when(commentRepository.countByArticleIds(List.of(201L))).thenReturn(List.of());
        when(articleReactionRepository.countByArticleIds(List.of(201L))).thenReturn(List.of());

        // When: 추천 글을 조회하면
        List<ArticleRecommendedItemResponse> items = service.findRecommendedPublic(1);

        // Then: 트렌딩 기반 fallback 결과가 반환되어야 한다.
        assertThat(items).hasSize(1);
        assertThat(items.get(0).articleId()).isEqualTo(201L);
        assertThat(items.get(0).personalized()).isFalse();
        assertThat(items.get(0).recommendationReason()).isEqualTo("최근 반응이 뜨거운 글 기반");
    }

    // 추천 결과는 공지사항/문의 게시판 글을 제외해야 한다.
    @Test
    void findRecommendedPublic_excludes_notice_and_inquiry_boards() {
        // Given: 공지사항 글과 일반 게시판 글이 후보에 섞여 있다.
        ArticleRecommendationService service = createService();
        BoardEntity freeBoard = createBoard(1L, "자유게시판", "free");
        BoardEntity noticeBoard = createBoard(2L, "공지사항", "notice");
        ArticleCategoryEntity javaCategory = createCategory(11L, freeBoard, "java");
        UserEntity currentUser = createUser(9L, "current-user");
        UserEntity author = createUser(2L, "author");
        ArticleEntity bookmarkedArticle = createArticle(300L, freeBoard, author, javaCategory, "북마크 글", Instant.parse("2026-03-11T00:00:00Z"));
        ArticleEntity freeCandidate = createArticle(301L, freeBoard, author, javaCategory, "자유 글", Instant.parse("2026-03-13T00:00:00Z"));
        ArticleEntity noticeCandidate = createArticle(302L, noticeBoard, author, null, "공지 글", Instant.parse("2026-03-13T00:00:00Z"));
        ArticleBookmarkEntity bookmark = createBookmark(1L, currentUser, bookmarkedArticle);

        when(currentUserService.getOptionalUserId()).thenReturn(Optional.of(9L));
        when(articleRepository.findByBoardVisibilityAndBoardDeletedAtIsNullAndBoardSlugNotInAndVisibilityAndNoticeFalseAndDeletedAtIsNull(
            BoardVisibility.PUBLIC,
            new PublicArticleFeedPolicy().excludedBoardSlugs(),
            ContentVisibility.PUBLIC,
            PageRequest.of(0, 60)
        )).thenReturn(new SliceImpl<>(List.of(freeCandidate, noticeCandidate), PageRequest.of(0, 60), false));
        when(articleTrendingService.findTrendingPublic(com.mocktalkback.domain.article.type.ArticleTrendingWindow.DAY, 3)).thenReturn(List.of());
        when(articleBookmarkRepository.findTop20ByUserIdOrderByCreatedAtDescIdDesc(9L)).thenReturn(List.of(bookmark));
        when(articleReactionRepository.findTop20ByUserIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of());
        when(commentRepository.findTop20ByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(9L)).thenReturn(List.of());
        when(commentRepository.countByArticleIds(List.of(301L, 302L))).thenReturn(List.of());
        when(articleReactionRepository.countByArticleIds(List.of(301L, 302L))).thenReturn(List.of());

        // When: 추천 글을 조회하면
        List<ArticleRecommendedItemResponse> items = service.findRecommendedPublic(1);

        // Then: 공지사항 게시판 글은 제외되고 일반 공개 글만 남아야 한다.
        assertThat(items).extracting(ArticleRecommendedItemResponse::articleId).containsExactly(301L);
        assertThat(items).extracting(ArticleRecommendedItemResponse::boardSlug).doesNotContain("notice");
    }

    private ArticleRecommendationService createService() {
        return new ArticleRecommendationService(
            articleRepository,
            articleBookmarkRepository,
            articleReactionRepository,
            commentRepository,
            articleTrendingService,
            currentUserService,
            new PublicArticleFeedPolicy(),
            new AuthorDisplayResolver(),
            Clock.fixed(Instant.parse("2026-03-14T00:00:00Z"), ZoneId.of("UTC"))
        );
    }

    private BoardEntity createBoard(Long id, String boardName, String slug) {
        BoardEntity board = BoardEntity.builder()
            .boardName(boardName)
            .slug(slug)
            .description("테스트")
            .visibility(BoardVisibility.PUBLIC)
            .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private ArticleCategoryEntity createCategory(Long id, BoardEntity board, String categoryName) {
        ArticleCategoryEntity category = ArticleCategoryEntity.builder()
            .board(board)
            .categoryName(categoryName)
            .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private UserEntity createUser(Long id, String loginId) {
        RoleEntity role = RoleEntity.create("USER", 0, "테스트");
        UserEntity user = UserEntity.createLocal(role, loginId, loginId + "@test.com", "pw", loginId, loginId, loginId);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ArticleEntity createArticle(
        Long id,
        BoardEntity board,
        UserEntity user,
        ArticleCategoryEntity category,
        String title,
        Instant createdAt
    ) {
        ArticleEntity article = ArticleEntity.builder()
            .board(board)
            .user(user)
            .category(category)
            .visibility(ContentVisibility.PUBLIC)
            .title(title)
            .content("content")
            .contentSource("content")
            .contentFormat(com.mocktalkback.domain.article.type.ArticleContentFormat.HTML)
            .hit(10L)
            .notice(false)
            .build();
        ReflectionTestUtils.setField(article, "id", id);
        ReflectionTestUtils.setField(article, "createdAt", createdAt);
        ReflectionTestUtils.setField(article, "updatedAt", createdAt);
        return article;
    }

    private ArticleBookmarkEntity createBookmark(Long id, UserEntity user, ArticleEntity article) {
        ArticleBookmarkEntity bookmark = ArticleBookmarkEntity.builder()
            .user(user)
            .article(article)
            .build();
        ReflectionTestUtils.setField(bookmark, "id", id);
        return bookmark;
    }

    @SuppressWarnings("unused")
    private ArticleReactionEntity createReaction(Long id, UserEntity user, ArticleEntity article, short reactionType) {
        ArticleReactionEntity reaction = ArticleReactionEntity.builder()
            .user(user)
            .article(article)
            .reactionType(reactionType)
            .build();
        ReflectionTestUtils.setField(reaction, "id", id);
        return reaction;
    }

    @SuppressWarnings("unused")
    private CommentEntity createComment(Long id, UserEntity user, ArticleEntity article) {
        CommentEntity comment = CommentEntity.builder()
            .user(user)
            .article(article)
            .parentComment(null)
            .rootComment(null)
            .depth(0)
            .content("content")
            .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
