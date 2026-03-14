package com.mocktalkback.domain.article.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.article.dto.ArticleRecommendedItemResponse;
import com.mocktalkback.domain.article.dto.ArticleTrendingItemResponse;
import com.mocktalkback.domain.article.entity.ArticleBookmarkEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleReactionEntity;
import com.mocktalkback.domain.article.policy.PublicArticleFeedPolicy;
import com.mocktalkback.domain.article.repository.ArticleBookmarkRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository.ArticleReactionCountView;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.article.type.ArticleTrendingWindow;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.common.policy.AuthorDisplayResolver;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.global.auth.CurrentUserService;

@Service
public class ArticleRecommendationService {

    private static final int DEFAULT_LIMIT = 9;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_CANDIDATE_FETCH_SIZE = 60;
    private static final int CANDIDATE_FETCH_MULTIPLIER = 6;
    private static final double BOOKMARK_BOARD_SCORE = 5.0d;
    private static final double BOOKMARK_CATEGORY_SCORE = 3.0d;
    private static final double LIKE_BOARD_SCORE = 4.0d;
    private static final double LIKE_CATEGORY_SCORE = 2.0d;
    private static final double DISLIKE_BOARD_SCORE = -4.0d;
    private static final double DISLIKE_CATEGORY_SCORE = -2.0d;
    private static final double COMMENT_BOARD_SCORE = 2.0d;
    private static final double COMMENT_CATEGORY_SCORE = 1.0d;
    private static final String BOOKMARK_BOARD_REASON = "북마크한 글과 비슷한 게시판 기반";
    private static final String BOOKMARK_CATEGORY_REASON = "북마크한 글과 비슷한 주제 기반";
    private static final String LIKE_BOARD_REASON = "최근 좋아요한 글과 비슷한 게시판 기반";
    private static final String LIKE_CATEGORY_REASON = "최근 좋아요한 글과 비슷한 주제 기반";
    private static final String COMMENT_BOARD_REASON = "최근 댓글을 남긴 게시판 기반";
    private static final String COMMENT_CATEGORY_REASON = "최근 댓글을 남긴 주제 기반";
    private static final String TRENDING_FALLBACK_REASON = "최근 반응이 뜨거운 글 기반";
    private static final String LOGIN_FALLBACK_REASON = "활동 이력이 적어 인기글 기반으로 추천";

    private final ArticleRepository articleRepository;
    private final ArticleBookmarkRepository articleBookmarkRepository;
    private final ArticleReactionRepository articleReactionRepository;
    private final CommentRepository commentRepository;
    private final ArticleTrendingService articleTrendingService;
    private final CurrentUserService currentUserService;
    private final PublicArticleFeedPolicy publicArticleFeedPolicy;
    private final AuthorDisplayResolver authorDisplayResolver;
    private final Clock clock;

    @Autowired
    public ArticleRecommendationService(
        ArticleRepository articleRepository,
        ArticleBookmarkRepository articleBookmarkRepository,
        ArticleReactionRepository articleReactionRepository,
        CommentRepository commentRepository,
        ArticleTrendingService articleTrendingService,
        CurrentUserService currentUserService,
        PublicArticleFeedPolicy publicArticleFeedPolicy,
        AuthorDisplayResolver authorDisplayResolver
    ) {
        this(
            articleRepository,
            articleBookmarkRepository,
            articleReactionRepository,
            commentRepository,
            articleTrendingService,
            currentUserService,
            publicArticleFeedPolicy,
            authorDisplayResolver,
            Clock.systemUTC()
        );
    }

    ArticleRecommendationService(
        ArticleRepository articleRepository,
        ArticleBookmarkRepository articleBookmarkRepository,
        ArticleReactionRepository articleReactionRepository,
        CommentRepository commentRepository,
        ArticleTrendingService articleTrendingService,
        CurrentUserService currentUserService,
        PublicArticleFeedPolicy publicArticleFeedPolicy,
        AuthorDisplayResolver authorDisplayResolver,
        Clock clock
    ) {
        this.articleRepository = articleRepository;
        this.articleBookmarkRepository = articleBookmarkRepository;
        this.articleReactionRepository = articleReactionRepository;
        this.commentRepository = commentRepository;
        this.articleTrendingService = articleTrendingService;
        this.currentUserService = currentUserService;
        this.publicArticleFeedPolicy = publicArticleFeedPolicy;
        this.authorDisplayResolver = authorDisplayResolver;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ArticleRecommendedItemResponse> findRecommendedPublic(int limit) {
        int resolvedLimit = normalizeLimit(limit);
        CandidateContext candidateContext = buildCandidateContext(resolvedLimit);
        if (candidateContext.candidateMap().isEmpty()) {
            return List.of();
        }

        Optional<Long> optionalUserId = currentUserService.getOptionalUserId();
        if (optionalUserId.isEmpty()) {
            return buildFallbackRecommendations(candidateContext, null, resolvedLimit, TRENDING_FALLBACK_REASON, Set.of());
        }

        Long userId = optionalUserId.get();
        UserSignalProfile signalProfile = buildUserSignalProfile(userId);
        if (!signalProfile.hasPositiveSignal()) {
            return buildFallbackRecommendations(
                candidateContext,
                userId,
                resolvedLimit,
                LOGIN_FALLBACK_REASON,
                signalProfile.excludedArticleIds()
            );
        }

        List<ArticleRecommendedItemResponse> personalizedItems = buildPersonalizedRecommendations(
            candidateContext,
            signalProfile,
            userId,
            resolvedLimit
        );
        if (personalizedItems.size() >= resolvedLimit) {
            return personalizedItems;
        }

        Set<Long> excludedIds = new LinkedHashSet<>(signalProfile.excludedArticleIds());
        for (ArticleRecommendedItemResponse item : personalizedItems) {
            excludedIds.add(item.articleId());
        }

        List<ArticleRecommendedItemResponse> fallbackItems = buildFallbackRecommendations(
            candidateContext,
            userId,
            resolvedLimit - personalizedItems.size(),
            TRENDING_FALLBACK_REASON,
            excludedIds
        );

        List<ArticleRecommendedItemResponse> mergedItems = new ArrayList<>(personalizedItems);
        mergedItems.addAll(fallbackItems);
        return mergedItems;
    }

    private CandidateContext buildCandidateContext(int limit) {
        int candidateFetchSize = Math.max(MIN_CANDIDATE_FETCH_SIZE, limit * CANDIDATE_FETCH_MULTIPLIER);
        List<ArticleEntity> recentCandidates = loadRecentPublicCandidates(candidateFetchSize);
        List<ArticleTrendingItemResponse> trendingItems = articleTrendingService.findTrendingPublic(
            ArticleTrendingWindow.DAY,
            Math.max(limit * 3, limit)
        );

        LinkedHashMap<Long, ArticleEntity> candidateMap = new LinkedHashMap<>();
        for (ArticleEntity article : recentCandidates) {
            candidateMap.put(article.getId(), article);
        }

        List<Long> missingTrendingIds = trendingItems.stream()
            .map(ArticleTrendingItemResponse::articleId)
            .filter(articleId -> !candidateMap.containsKey(articleId))
            .toList();
        if (!missingTrendingIds.isEmpty()) {
            Map<Long, ArticleEntity> extraTrendingMap = articleRepository.findAllByIdInAndDeletedAtIsNull(missingTrendingIds).stream()
                .filter(publicArticleFeedPolicy::isPublicFeedTarget)
                .collect(LinkedHashMap::new, (map, article) -> map.put(article.getId(), article), Map::putAll);
            for (Long articleId : missingTrendingIds) {
                ArticleEntity article = extraTrendingMap.get(articleId);
                if (article != null) {
                    candidateMap.putIfAbsent(articleId, article);
                }
            }
        }

        List<Long> candidateIds = List.copyOf(candidateMap.keySet());
        Map<Long, Long> commentCounts = loadCommentCounts(candidateIds);
        Map<Long, ReactionCounts> reactionCounts = loadReactionCounts(candidateIds);
        Map<Long, Double> trendingScoreMap = new HashMap<>();
        for (ArticleTrendingItemResponse trendingItem : trendingItems) {
            trendingScoreMap.put(trendingItem.articleId(), trendingItem.trendScore());
        }

        return new CandidateContext(candidateMap, trendingItems, commentCounts, reactionCounts, trendingScoreMap);
    }

    private List<ArticleRecommendedItemResponse> buildPersonalizedRecommendations(
        CandidateContext candidateContext,
        UserSignalProfile signalProfile,
        Long userId,
        int limit
    ) {
        List<ScoredRecommendation> scoredRecommendations = new ArrayList<>();
        for (ArticleEntity article : candidateContext.candidateMap().values()) {
            if (!isVisibleToRecommendedUser(article, userId, signalProfile.excludedArticleIds())) {
                continue;
            }

            double boardScore = signalProfile.boardScores().getOrDefault(article.getBoard().getId(), 0.0d);
            double categoryScore = 0.0d;
            if (article.getCategory() != null) {
                categoryScore = signalProfile.categoryScores().getOrDefault(article.getCategory().getId(), 0.0d);
            }
            double interestScore = boardScore + categoryScore;
            if (interestScore <= 0.0d) {
                continue;
            }

            double recencyBonus = resolveRecencyBonus(article.getCreatedAt());
            double trendBonus = resolveTrendBonus(candidateContext.trendingScoreMap().getOrDefault(article.getId(), 0.0d));
            double recommendationScore = interestScore + recencyBonus + trendBonus;
            String recommendationReason = resolvePersonalizedReason(signalProfile, article, boardScore, categoryScore, trendBonus);
            scoredRecommendations.add(new ScoredRecommendation(article.getId(), recommendationScore, recommendationReason, true));
        }

        scoredRecommendations.sort((left, right) -> {
            int scoreCompare = Double.compare(right.recommendationScore(), left.recommendationScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }

            ArticleEntity rightArticle = candidateContext.candidateMap().get(right.articleId());
            ArticleEntity leftArticle = candidateContext.candidateMap().get(left.articleId());
            int createdAtCompare = rightArticle.getCreatedAt().compareTo(leftArticle.getCreatedAt());
            if (createdAtCompare != 0) {
                return createdAtCompare;
            }
            return rightArticle.getId().compareTo(leftArticle.getId());
        });

        List<ArticleRecommendedItemResponse> items = new ArrayList<>();
        for (ScoredRecommendation scoredRecommendation : scoredRecommendations) {
            if (items.size() >= limit) {
                break;
            }
            ArticleEntity article = candidateContext.candidateMap().get(scoredRecommendation.articleId());
            if (article != null) {
                items.add(toRecommendedItemResponse(article, candidateContext, scoredRecommendation));
            }
        }
        return items;
    }

    private List<ArticleRecommendedItemResponse> buildFallbackRecommendations(
        CandidateContext candidateContext,
        Long userId,
        int limit,
        String fallbackReason,
        Set<Long> excludedIds
    ) {
        if (limit <= 0) {
            return List.of();
        }

        LinkedHashSet<Long> selectedIds = new LinkedHashSet<>(excludedIds);
        List<ArticleRecommendedItemResponse> items = new ArrayList<>();

        for (ArticleTrendingItemResponse trendingItem : candidateContext.trendingItems()) {
            if (items.size() >= limit) {
                break;
            }

            ArticleEntity article = candidateContext.candidateMap().get(trendingItem.articleId());
            if (article == null || !isVisibleToRecommendedUser(article, userId, selectedIds)) {
                continue;
            }

            selectedIds.add(article.getId());
            double recommendationScore = resolveTrendBonus(trendingItem.trendScore()) + resolveRecencyBonus(article.getCreatedAt());
            items.add(toRecommendedItemResponse(
                article,
                candidateContext,
                new ScoredRecommendation(article.getId(), recommendationScore, fallbackReason, false)
            ));
        }

        if (items.size() >= limit) {
            return items;
        }

        List<ArticleEntity> recentCandidates = new ArrayList<>(candidateContext.candidateMap().values());
        recentCandidates.sort((left, right) -> {
            int createdAtCompare = right.getCreatedAt().compareTo(left.getCreatedAt());
            if (createdAtCompare != 0) {
                return createdAtCompare;
            }
            return right.getId().compareTo(left.getId());
        });

        for (ArticleEntity article : recentCandidates) {
            if (items.size() >= limit) {
                break;
            }
            if (!isVisibleToRecommendedUser(article, userId, selectedIds)) {
                continue;
            }

            selectedIds.add(article.getId());
            double recommendationScore = resolveTrendBonus(candidateContext.trendingScoreMap().getOrDefault(article.getId(), 0.0d))
                + resolveRecencyBonus(article.getCreatedAt());
            items.add(toRecommendedItemResponse(
                article,
                candidateContext,
                new ScoredRecommendation(article.getId(), recommendationScore, fallbackReason, false)
            ));
        }

        return items;
    }

    private UserSignalProfile buildUserSignalProfile(Long userId) {
        Map<Long, Double> boardScores = new HashMap<>();
        Map<Long, Double> categoryScores = new HashMap<>();
        Map<Long, String> boardReasons = new HashMap<>();
        Map<Long, String> categoryReasons = new HashMap<>();
        LinkedHashSet<Long> excludedArticleIds = new LinkedHashSet<>();

        List<ArticleBookmarkEntity> bookmarks = articleBookmarkRepository.findTop20ByUserIdOrderByCreatedAtDescIdDesc(userId);
        for (ArticleBookmarkEntity bookmark : bookmarks) {
            ArticleEntity article = bookmark.getArticle();
            if (!publicArticleFeedPolicy.isPublicFeedTarget(article)) {
                continue;
            }

            excludedArticleIds.add(article.getId());
            addPositiveSignal(boardScores, boardReasons, article.getBoard().getId(), BOOKMARK_BOARD_SCORE, BOOKMARK_BOARD_REASON);
            if (article.getCategory() != null) {
                addPositiveSignal(categoryScores, categoryReasons, article.getCategory().getId(), BOOKMARK_CATEGORY_SCORE, BOOKMARK_CATEGORY_REASON);
            }
        }

        List<ArticleReactionEntity> reactions = articleReactionRepository.findTop20ByUserIdOrderByUpdatedAtDescIdDesc(userId);
        for (ArticleReactionEntity reaction : reactions) {
            ArticleEntity article = reaction.getArticle();
            if (!publicArticleFeedPolicy.isPublicFeedTarget(article)) {
                continue;
            }

            if (reaction.getReactionType() == 1) {
                addPositiveSignal(boardScores, boardReasons, article.getBoard().getId(), LIKE_BOARD_SCORE, LIKE_BOARD_REASON);
                if (article.getCategory() != null) {
                    addPositiveSignal(categoryScores, categoryReasons, article.getCategory().getId(), LIKE_CATEGORY_SCORE, LIKE_CATEGORY_REASON);
                }
            } else if (reaction.getReactionType() == -1) {
                addNegativeSignal(boardScores, article.getBoard().getId(), DISLIKE_BOARD_SCORE);
                if (article.getCategory() != null) {
                    addNegativeSignal(categoryScores, article.getCategory().getId(), DISLIKE_CATEGORY_SCORE);
                }
            }
        }

        List<CommentEntity> comments = commentRepository.findTop20ByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(userId);
        for (CommentEntity comment : comments) {
            ArticleEntity article = comment.getArticle();
            if (!publicArticleFeedPolicy.isPublicFeedTarget(article)) {
                continue;
            }

            addPositiveSignal(boardScores, boardReasons, article.getBoard().getId(), COMMENT_BOARD_SCORE, COMMENT_BOARD_REASON);
            if (article.getCategory() != null) {
                addPositiveSignal(categoryScores, categoryReasons, article.getCategory().getId(), COMMENT_CATEGORY_SCORE, COMMENT_CATEGORY_REASON);
            }
        }

        return new UserSignalProfile(boardScores, categoryScores, boardReasons, categoryReasons, excludedArticleIds);
    }

    private List<ArticleEntity> loadRecentPublicCandidates(int size) {
        Pageable pageable = PageRequest.of(0, size);
        return articleRepository
            .findByBoardVisibilityAndBoardDeletedAtIsNullAndBoardSlugNotInAndVisibilityAndNoticeFalseAndDeletedAtIsNull(
                BoardVisibility.PUBLIC,
                publicArticleFeedPolicy.excludedBoardSlugs(),
                ContentVisibility.PUBLIC,
                pageable
            )
            .getContent();
    }

    private Map<Long, Long> loadCommentCounts(Collection<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.countByArticleIds(articleIds).stream()
            .collect(LinkedHashMap::new, (map, row) -> map.put(row.getArticleId(), row.getCount()), Map::putAll);
    }

    private Map<Long, ReactionCounts> loadReactionCounts(Collection<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ReactionCounts> counts = new HashMap<>();
        List<ArticleReactionCountView> rows = articleReactionRepository.countByArticleIds(articleIds);
        for (ArticleReactionCountView row : rows) {
            ReactionCounts current = counts.getOrDefault(row.getArticleId(), ReactionCounts.empty());
            if (row.getReactionType() == 1) {
                counts.put(row.getArticleId(), new ReactionCounts(current.likeCount() + row.getCount(), current.dislikeCount()));
            } else if (row.getReactionType() == -1) {
                counts.put(row.getArticleId(), new ReactionCounts(current.likeCount(), current.dislikeCount() + row.getCount()));
            }
        }
        return counts;
    }

    private ArticleRecommendedItemResponse toRecommendedItemResponse(
        ArticleEntity article,
        CandidateContext candidateContext,
        ScoredRecommendation scoredRecommendation
    ) {
        ReactionCounts reactionCounts = candidateContext.reactionCounts().getOrDefault(article.getId(), ReactionCounts.empty());
        return new ArticleRecommendedItemResponse(
            article.getId(),
            article.getBoard().getId(),
            article.getBoard().getSlug(),
            article.getBoard().getBoardName(),
            article.getUser().getId(),
            authorDisplayResolver.resolveAuthorName(article.getUser()),
            article.getTitle(),
            article.getHit(),
            candidateContext.commentCounts().getOrDefault(article.getId(), 0L),
            reactionCounts.likeCount(),
            reactionCounts.dislikeCount(),
            scoredRecommendation.recommendationScore(),
            scoredRecommendation.recommendationReason(),
            scoredRecommendation.personalized(),
            article.getCreatedAt()
        );
    }

    private boolean isVisibleToRecommendedUser(ArticleEntity article, Long userId, Set<Long> excludedIds) {
        if (article == null || excludedIds.contains(article.getId())) {
            return false;
        }
        if (!publicArticleFeedPolicy.isPublicFeedTarget(article)) {
            return false;
        }
        return userId == null || !userId.equals(article.getUser().getId());
    }

    private String resolvePersonalizedReason(
        UserSignalProfile signalProfile,
        ArticleEntity article,
        double boardScore,
        double categoryScore,
        double trendBonus
    ) {
        String boardReason = signalProfile.boardReasons().get(article.getBoard().getId());
        String categoryReason = article.getCategory() == null ? null : signalProfile.categoryReasons().get(article.getCategory().getId());
        if (categoryScore > boardScore && categoryReason != null) {
            return categoryReason;
        }
        if (boardScore > 0.0d && boardReason != null) {
            return boardReason;
        }
        if (categoryScore > 0.0d && categoryReason != null) {
            return categoryReason;
        }
        if (trendBonus > 0.0d) {
            return TRENDING_FALLBACK_REASON;
        }
        return LOGIN_FALLBACK_REASON;
    }

    private void addPositiveSignal(
        Map<Long, Double> scoreMap,
        Map<Long, String> reasonMap,
        Long key,
        double delta,
        String reason
    ) {
        scoreMap.put(key, scoreMap.getOrDefault(key, 0.0d) + delta);
        reasonMap.putIfAbsent(key, reason);
    }

    private void addNegativeSignal(Map<Long, Double> scoreMap, Long key, double delta) {
        scoreMap.put(key, scoreMap.getOrDefault(key, 0.0d) + delta);
    }

    private double resolveRecencyBonus(Instant createdAt) {
        if (createdAt == null) {
            return 0.0d;
        }

        Duration age = Duration.between(createdAt, clock.instant());
        if (age.isNegative() || age.compareTo(Duration.ofDays(1)) <= 0) {
            return 3.0d;
        }
        if (age.compareTo(Duration.ofDays(3)) <= 0) {
            return 2.0d;
        }
        if (age.compareTo(Duration.ofDays(7)) <= 0) {
            return 1.0d;
        }
        return 0.0d;
    }

    private double resolveTrendBonus(double trendScore) {
        if (trendScore <= 0.0d) {
            return 0.0d;
        }
        if (trendScore >= 20.0d) {
            return 3.0d;
        }
        if (trendScore >= 10.0d) {
            return 2.0d;
        }
        return 1.0d;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private record ReactionCounts(long likeCount, long dislikeCount) {
        private static ReactionCounts empty() {
            return new ReactionCounts(0L, 0L);
        }
    }

    private record CandidateContext(
        Map<Long, ArticleEntity> candidateMap,
        List<ArticleTrendingItemResponse> trendingItems,
        Map<Long, Long> commentCounts,
        Map<Long, ReactionCounts> reactionCounts,
        Map<Long, Double> trendingScoreMap
    ) {
    }

    private record ScoredRecommendation(
        Long articleId,
        double recommendationScore,
        String recommendationReason,
        boolean personalized
    ) {
    }

    private record UserSignalProfile(
        Map<Long, Double> boardScores,
        Map<Long, Double> categoryScores,
        Map<Long, String> boardReasons,
        Map<Long, String> categoryReasons,
        Set<Long> excludedArticleIds
    ) {
        private boolean hasPositiveSignal() {
            return boardScores.values().stream().anyMatch(score -> score > 0.0d)
                || categoryScores.values().stream().anyMatch(score -> score > 0.0d);
        }
    }
}
