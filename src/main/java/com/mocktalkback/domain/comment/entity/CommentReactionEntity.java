package com.mocktalkback.domain.comment.entity;

import com.mocktalkback.global.common.entity.BaseTimeEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_comment_reactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_comment_reactions_user_id_comment_id",
            columnNames = {"user_id", "comment_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_comment_reactions_comment_id", columnList = "comment_id")
    }
)
public class CommentReactionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_reaction_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_comment_reactions_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "comment_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_comment_reactions_comment_id__tb_comments")
    )
    private CommentEntity comment;

    @Column(name = "reaction_type", nullable = false)
    private short reactionType;

    @Builder
    private CommentReactionEntity(UserEntity user, CommentEntity comment, short reactionType) {
        this.user = user;
        this.comment = comment;
        this.reactionType = reactionType;
    }
}
