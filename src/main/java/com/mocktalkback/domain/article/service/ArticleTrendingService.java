package com.mocktalkback.domain.article.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mocktalkback.domain.article.dto.ArticleTrendingItemResponse;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository;
import com.mocktalkback.domain.article.repository.ArticleReactionRepository.ArticleReactionCountView;
import com.mocktalkback.domain.article.repository.ArticleRepository;
import com.mocktalkback.domain.article.type.ArticleTrendingWindow;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.comment.repository.CommentRepository;
import com.mocktalkback.domain.common.policy.AuthorDisplayResolver;
import com.mocktalkback.domain.role.type.ContentVisibility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ArticleTrendingService {

    private static final int MAX_TRENDING_LIMIT = 50;
    private static final int FETCH_MULTIPLIER = 3;
    private static final double VIEW_SCORE = 1.0d;
    private static final double COMMENT_SCORE = 4.0d;
    private static final double LIKE_SCORE = 3.0d;
    private static final double DISLIKE_SCORE = -2.0d;
    private static final double BOOKMARK_SCORE = 5.0d;
    private static final Duration HOUR_TTL = Duration.ofDays(2);
    private static final Duration DAY_TTL = Duration.ofDays(8);
    private static final Duration WEEK_TTL = Duration.ofDays(42);
    private static final ZoneId TREND_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH", Locale.ROOT);
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("YYYYww", Locale.ROOT);

    private final ArticleTrendingStore articleTrendingStore;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ArticleReactionRepository articleReactionRepository;
    private final AuthorDisplayResolver authorDisplayResolver;
    private final Clock clock;

    @Autowired
    public ArticleTrendingService(
        ArticleTrendingStore articleTrendingStore,
        ArticleRepository articleRepository,
        CommentRepository commentRepository,
        ArticleReactionRepository articleReactionRepository,
        AuthorDisplayResolver authorDisplayResolver
    ) {
        this(
            articleTrendingStore,
            articleRepository,
            commentRepository,
            articleReactionRepository,
            authorDisplayResolver,
            Clock.system(TREND_ZONE)
        );
    }

    ArticleTrendingService(
        ArticleTrendingStore articleTrendingStore,
        ArticleRepository articleRepository,
        CommentRepository commentRepository,
        ArticleReactionRepository articleReactionRepository,
        AuthorDisplayResolver authorDisplayResolver,
        Clock clock
    ) {
        this.articleTrendingStore = articleTrendingStore;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.articleReactionRepository = articleReactionRepository;
        this.authorDisplayResolver = authorDisplayResolver;
        this.clock = clock;
    }

    public void recordView(Long articleId) {
        applyScore(articleId, VIEW_SCORE);
    }

    public void recordCommentCreated(Long articleId) {
        applyScore(articleId, COMMENT_SCORE);
    }

    public void recordCommentDeleted(Long articleId) {
        applyScore(articleId, -COMMENT_SCORE);
    }

    public void recordBookmarkCreated(Long articleId) {
        applyScore(articleId, BOOKMARK_SCORE);
    }

    public void recordBookmarkDeleted(Long articleId) {
        applyScore(articleId, -BOOKMARK_SCORE);
    }

    public void recordArticleReactionChanged(Long articleId, short previousReaction, short currentReaction) {
        double delta = resolveReactionScore(currentReaction) - resolveReactionScore(previousReaction);
        applyScore(articleId, delta);
    }

    public List<ArticleTrendingItemResponse> findTrendingPublic(ArticleTrendingWindow window, int limit) {
        int resolvedLimit = normalizeLimit(limit);
        List<ArticleTrendingStore.RankedArticle> rankedArticles = resolveRankedArticles(window, resolvedLimit);
        if (rankedArticles.isEmpty()) {
            return List.of();
        }

        List<Long> articleIds = rankedArticles.stream()
            .map(ArticleTrendingStore.RankedArticle::articleId)
            .toList();
        Map<Long, ArticleEntity> articleMap = loadPublicArticleMap(articleIds);
        if (articleMap.isEmpty()) {
            return List.of();
        }

        List<Long> visibleArticleIds = articleIds.stream()
            .filter(articleMap::containsKey)
            .toList();
        Map<Long, Long> commentCounts = loadCommentCounts(visibleArticleIds);
        Map<Long, ReactionCounts> reactionCounts = loadReactionCounts(visibleArticleIds);

        Map<Long, Double> scoreMap = new HashMap<>();
        for (ArticleTrendingStore.RankedArticle rankedArticle : rankedArticles) {
            scoreMap.put(rankedArticle.articleId(), rankedArticle.score());
        }

        Map<Long, ArticleTrendingItemResponse> responseMap = new LinkedHashMap<>();
        for (Long articleId : articleIds) {
            ArticleEntity article = articleMap.get(articleId);
            if (article == null) {
                continue;
            }
            ReactionCounts counts = reactionCounts.getOrDefault(articleId, ReactionCounts.empty());
            responseMap.put(articleId, new ArticleTrendingItemResponse(
                article.getId(),
                article.getBoard().getId(),
                article.getBoard().getSlug(),
                article.getUser().getId(),
                authorDisplayResolver.resolveAuthorName(article.getUser()),
                article.getTitle(),
                article.getHit(),
                commentCounts.getOrDefault(articleId, 0L),
                counts.likeCount(),
                counts.dislikeCount(),
                scoreMap.getOrDefault(articleId, 0.0d),
                article.getCreatedAt()
            ));
            if (responseMap.size() >= resolvedLimit) {
                break;
            }
        }
        return List.copyOf(responseMap.values());
    }

    private void applyScore(Long articleId, double delta) {
        if (articleId == null || delta == 0.0d) {
            return;
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), TREND_ZONE);
        try {
            articleTrendingStore.incrementScore(hourKey(now), articleId, delta, HOUR_TTL);
            articleTrendingStore.incrementScore(dayKey(now), articleId, delta, DAY_TTL);
            articleTrendingStore.incrementScore(weekKey(now), articleId, delta, WEEK_TTL);
        } catch (Exception ex) {
            log.warn("게시글 트렌딩 점수 적재에 실패했습니다. articleId={}, delta={}", articleId, delta, ex);
        }
    }

    private List<ArticleTrendingStore.RankedArticle> resolveRankedArticles(ArticleTrendingWindow window, int limit) {
        ArticleTrendingWindow resolvedWindow = window == null ? ArticleTrendingWindow.DAY : window;
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), TREND_ZONE);
        String key = switch (resolvedWindow) {
            case WEEK -> weekKey(now);
            case DAY -> dayKey(now);
        };

        try {
            return articleTrendingStore.findTopArticles(key, Math.max(limit * FETCH_MULTIPLIER, limit));
        } catch (Exception ex) {
            log.warn("게시글 트렌딩 조회에 실패했습니다. window={}", resolvedWindow, ex);
            return List.of();
        }
    }

    private Map<Long, ArticleEntity> loadPublicArticleMap(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        return articleRepository.findAllByIdInAndDeletedAtIsNull(articleIds).stream()
            .filter(this::isPublicTrendingTarget)
            .collect(LinkedHashMap::new, (map, article) -> map.put(article.getId(), article), Map::putAll);
    }

    private boolean isPublicTrendingTarget(ArticleEntity article) {
        BoardEntity board = article.getBoard();
        return !article.isDeleted()
            && article.getVisibility() == ContentVisibility.PUBLIC
            && board != null
            && !board.isDeleted()
            && board.getVisibility() == BoardVisibility.PUBLIC;
    }

    private Map<Long, Long> loadCommentCounts(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countByArticleIds(articleIds).stream()
            .collect(LinkedHashMap::new, (map, view) -> map.put(view.getArticleId(), view.getCount()), Map::putAll);
    }

    private Map<Long, ReactionCounts> loadReactionCounts(List<Long> articleIds) {
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

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, MAX_TRENDING_LIMIT);
    }

    private double resolveReactionScore(short reactionType) {
        return switch (reactionType) {
            case 1 -> LIKE_SCORE;
            case -1 -> DISLIKE_SCORE;
            default -> 0.0d;
        };
    }

    private String hourKey(LocalDateTime dateTime) {
        return "trend:article:hour:" + HOUR_FORMATTER.format(dateTime);
    }

    private String dayKey(LocalDateTime dateTime) {
        return "trend:article:day:" + DAY_FORMATTER.format(dateTime);
    }

    private String weekKey(LocalDateTime dateTime) {
        return "trend:article:week:" + WEEK_FORMATTER.format(dateTime);
    }

    private record ReactionCounts(long likeCount, long dislikeCount) {
        private static ReactionCounts empty() {
            return new ReactionCounts(0L, 0L);
        }
    }
}
