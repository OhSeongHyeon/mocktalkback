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

    @Query(value = """
        with upsert as (
            insert into tb_comment_reactions (user_id, comment_id, reaction_type)
            values (:userId, :commentId, :reactionType)
            on conflict (user_id, comment_id)
            do update
            set reaction_type = case
                when tb_comment_reactions.reaction_type = excluded.reaction_type then 0
                else excluded.reaction_type
            end,
            updated_at = now()
            returning reaction_type
        ),
        cleanup as (
            delete from tb_comment_reactions
            where user_id = :userId
              and comment_id = :commentId
              and reaction_type = 0
            returning comment_reaction_id
        )
        select case
            when exists (select 1 from cleanup) then cast(0 as smallint)
            else (select reaction_type from upsert)
        end
        """, nativeQuery = true)
    short upsertToggleReaction(
        @Param("userId") Long userId,
        @Param("commentId") Long commentId,
        @Param("reactionType") short reactionType
    );

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
