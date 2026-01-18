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

    interface ArticleTitleView {
        Long getId();
        String getTitle();
    }
}
