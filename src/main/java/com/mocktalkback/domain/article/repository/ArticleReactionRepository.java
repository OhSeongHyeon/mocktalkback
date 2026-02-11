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

    @Query(value = """
        with upsert as (
            insert into tb_article_reactions (user_id, article_id, reaction_type)
            values (:userId, :articleId, :reactionType)
            on conflict (user_id, article_id)
            do update
            set reaction_type = case
                when tb_article_reactions.reaction_type = excluded.reaction_type then 0
                else excluded.reaction_type
            end,
            updated_at = now()
            returning reaction_type
        ),
        cleanup as (
            delete from tb_article_reactions
            where user_id = :userId
              and article_id = :articleId
              and reaction_type = 0
            returning article_reaction_id
        )
        select case
            when exists (select 1 from cleanup) then cast(0 as smallint)
            else (select reaction_type from upsert)
        end
        """, nativeQuery = true)
    short upsertToggleReaction(
        @Param("userId") Long userId,
        @Param("articleId") Long articleId,
        @Param("reactionType") short reactionType
    );

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
