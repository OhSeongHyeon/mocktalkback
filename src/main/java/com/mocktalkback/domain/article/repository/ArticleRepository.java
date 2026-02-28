package com.mocktalkback.domain.article.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.moderation.type.ReportTargetType;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.dto.MyArticleItemResponse;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    Page<ArticleEntity> findByUserId(Long userId, Pageable pageable);

    @Query("""
        select new com.mocktalkback.domain.user.dto.MyArticleItemResponse(
            a.id,
            b.id,
            b.slug,
            b.boardName,
            u.id,
            case
                when u.displayName is null or trim(u.displayName) = '' then u.userName
                else u.displayName
            end,
            c.id,
            a.visibility,
            a.title,
            a.hit,
            (
                select count(cm.id)
                from CommentEntity cm
                where cm.article.id = a.id
                  and cm.deletedAt is null
            ),
            (
                select count(ar.id)
                from ArticleReactionEntity ar
                where ar.article.id = a.id
                  and ar.reactionType = 1
            ),
            (
                select count(ar.id)
                from ArticleReactionEntity ar
                where ar.article.id = a.id
                  and ar.reactionType = -1
            ),
            a.notice,
            a.createdAt,
            a.updatedAt,
            a.deletedAt
        )
        from ArticleEntity a
        join a.board b
        join a.user u
        left join a.category c
        where a.user.id = :userId
          and a.deletedAt is null
        """)
    Page<MyArticleItemResponse> findMyArticleItems(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "board"})
    Optional<ArticleEntity> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"user"})
    Page<ArticleEntity> findByBoardIdAndNoticeFalseAndVisibilityInAndDeletedAtIsNull(
        Long boardId,
        Collection<ContentVisibility> visibilities,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    Page<ArticleEntity> findByBoardIdAndCategoryIdAndNoticeFalseAndVisibilityInAndDeletedAtIsNull(
        Long boardId,
        Long categoryId,
        Collection<ContentVisibility> visibilities,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    Page<ArticleEntity> findByBoardIdAndCategoryIsNullAndNoticeFalseAndVisibilityInAndDeletedAtIsNull(
        Long boardId,
        Collection<ContentVisibility> visibilities,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    List<ArticleEntity> findByBoardIdAndNoticeTrueAndVisibilityInAndDeletedAtIsNull(
        Long boardId,
        Collection<ContentVisibility> visibilities,
        Pageable pageable
    );

    @Query("""
        select a.id as id, a.title as title
        from ArticleEntity a
        where a.id in :articleIds
        """)
    List<ArticleTitleView> findTitlesByIdIn(@Param("articleIds") Collection<Long> articleIds);

    boolean existsByCategoryIdAndDeletedAtIsNull(Long categoryId);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        select a
        from ArticleEntity a
        join a.user u
        where a.board.id = :boardId
          and a.deletedAt is null
          and (:authorId is null or u.id = :authorId)
          and (:notice is null or a.notice = :notice)
          and (
            :reported is null
            or (:reported = true and exists (
              select 1
              from ReportEntity r
              where r.board.id = :boardId
                and r.targetType = :targetType
                and r.targetId = a.id
            ))
            or (:reported = false and not exists (
              select 1
              from ReportEntity r
              where r.board.id = :boardId
                and r.targetType = :targetType
                and r.targetId = a.id
            ))
          )
        """)
    Page<ArticleEntity> findAdminBoardArticles(
        @Param("boardId") Long boardId,
        @Param("authorId") Long authorId,
        @Param("notice") Boolean notice,
        @Param("reported") Boolean reported,
        @Param("targetType") ReportTargetType targetType,
        Pageable pageable
    );

    interface ArticleTitleView {
        Long getId();
        String getTitle();
    }
}
