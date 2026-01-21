package com.mocktalkback.domain.comment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.comment.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByUserId(Long userId, Pageable pageable);

    Optional<CommentEntity> findByIdAndDeletedAtIsNull(Long id);

    Page<CommentEntity> findByArticleIdAndParentCommentIsNull(Long articleId, Pageable pageable);

    @Query("""
        select c from CommentEntity c
        left join c.rootComment rc
        where c.article.id = :articleId
          and (rc.id in :rootIds or c.id in :rootIds)
        order by c.createdAt asc, c.id asc
        """)
    List<CommentEntity> findTreeByArticleIdAndRootIds(
        @Param("articleId") Long articleId,
        @Param("rootIds") Collection<Long> rootIds
    );

    @Query("""
        select c.article.id as articleId, count(c.id) as count
        from CommentEntity c
        where c.article.id in :articleIds and c.deletedAt is null
        group by c.article.id
        """)
    List<CommentCountView> countByArticleIds(@Param("articleIds") Collection<Long> articleIds);

    @Query("""
        select c.id as commentId, c.article.id as articleId
        from CommentEntity c
        where c.id in :commentIds
        """)
    List<CommentArticleView> findArticleIdsByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    @EntityGraph(attributePaths = {"user", "article"})
    @Query("""
        select c
        from CommentEntity c
        join c.article a
        join c.user u
        where a.board.id = :boardId
          and c.deletedAt is null
          and (:authorId is null or u.id = :authorId)
          and (
            :reported is null
            or (:reported = true and exists (
              select 1
              from ReportEntity r
              where r.board.id = :boardId
                and r.targetType = :targetType
                and r.targetId = c.id
            ))
            or (:reported = false and not exists (
              select 1
              from ReportEntity r
              where r.board.id = :boardId
                and r.targetType = :targetType
                and r.targetId = c.id
            ))
          )
        """)
    Page<CommentEntity> findAdminBoardComments(
        @Param("boardId") Long boardId,
        @Param("authorId") Long authorId,
        @Param("reported") Boolean reported,
        @Param("targetType") com.mocktalkback.domain.moderation.type.ReportTargetType targetType,
        Pageable pageable
    );

    interface CommentCountView {
        Long getArticleId();
        long getCount();
    }

    interface CommentArticleView {
        Long getCommentId();
        Long getArticleId();
    }
}
