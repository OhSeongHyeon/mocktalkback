package com.mocktalkback.domain.article.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    Page<ArticleEntity> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "board"})
    Optional<ArticleEntity> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"user"})
    Page<ArticleEntity> findByBoardIdAndNoticeFalseAndVisibilityInAndDeletedAtIsNull(
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
        @Param("targetType") com.mocktalkback.domain.moderation.type.ReportTargetType targetType,
        Pageable pageable
    );

    interface ArticleTitleView {
        Long getId();
        String getTitle();
    }
}
