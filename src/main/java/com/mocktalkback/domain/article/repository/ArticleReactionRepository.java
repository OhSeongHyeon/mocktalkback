package com.mocktalkback.domain.article.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.article.entity.ArticleReactionEntity;

public interface ArticleReactionRepository extends JpaRepository<ArticleReactionEntity, Long> {
    Optional<ArticleReactionEntity> findByUserIdAndArticleId(Long userId, Long articleId);

    long countByArticleIdAndReactionType(Long articleId, short reactionType);

    @Query("""
        select ar.article.id as articleId, ar.reactionType as reactionType, count(ar.id) as count
        from ArticleReactionEntity ar
        where ar.article.id in :articleIds
        group by ar.article.id, ar.reactionType
        """)
    List<ArticleReactionCountView> countByArticleIds(@Param("articleIds") Collection<Long> articleIds);

    interface ArticleReactionCountView {
        Long getArticleId();
        short getReactionType();
        long getCount();
    }
}
