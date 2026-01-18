package com.mocktalkback.domain.comment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.comment.entity.CommentReactionEntity;

public interface CommentReactionRepository extends JpaRepository<CommentReactionEntity, Long> {
    Optional<CommentReactionEntity> findByUserIdAndCommentId(Long userId, Long commentId);

    long countByCommentIdAndReactionType(Long commentId, short reactionType);

    @Query("""
        select cr.comment.id as commentId, cr.reactionType as reactionType
        from CommentReactionEntity cr
        where cr.user.id = :userId and cr.comment.id in :commentIds
        """)
    List<CommentReactionUserView> findUserReactions(
        @Param("userId") Long userId,
        @Param("commentIds") Collection<Long> commentIds
    );

    @Query("""
        select cr.comment.id as commentId, cr.reactionType as reactionType, count(cr.id) as count
        from CommentReactionEntity cr
        where cr.comment.id in :commentIds
        group by cr.comment.id, cr.reactionType
        """)
    List<CommentReactionCountView> countByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    interface CommentReactionCountView {
        Long getCommentId();
        short getReactionType();
        long getCount();
    }

    interface CommentReactionUserView {
        Long getCommentId();
        short getReactionType();
    }
}
