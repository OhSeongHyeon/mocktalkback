package com.mocktalkback.domain.article.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleBookmarkEntity;

public interface ArticleBookmarkRepository extends JpaRepository<ArticleBookmarkEntity, Long> {
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    void deleteByUserIdAndArticleId(Long userId, Long articleId);

    @EntityGraph(attributePaths = {"article", "article.board", "article.user"})
    Page<ArticleBookmarkEntity> findAllByUserIdAndArticleDeletedAtIsNullAndArticleBoardDeletedAtIsNull(
        Long userId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"article", "article.board", "article.user", "article.category"})
    List<ArticleBookmarkEntity> findTop20ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndArticleIdIn(Long userId, Iterable<Long> articleIds);
}
