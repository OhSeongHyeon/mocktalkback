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

    @Query("""
        select c.article.id as articleId, count(c.id) as count
        from CommentEntity c
        where c.article.id in :articleIds and c.deletedAt is null
        group by c.article.id
        """)
    List<CommentCountView> countByArticleIds(@Param("articleIds") Collection<Long> articleIds);

    interface CommentCountView {
        Long getArticleId();
        long getCount();
    }
}
