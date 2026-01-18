package com.mocktalkback.domain.comment.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.comment.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByUserId(Long userId, Pageable pageable);

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

    interface CommentCountView {
        Long getArticleId();
        long getCount();
    }

    interface CommentArticleView {
        Long getCommentId();
        Long getArticleId();
    }
}
