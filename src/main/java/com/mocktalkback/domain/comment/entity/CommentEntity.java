package com.mocktalkback.domain.comment.entity;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.global.common.entity.SoftDeleteEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_comments")
public class CommentEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_comments_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "article_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_comments_article_id__tb_articles")
    )
    private ArticleEntity article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "parent_comment_id",
        foreignKey = @ForeignKey(name = "fk_tb_comments_parent_comment_id__tb_comments")
    )
    private CommentEntity parentComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "root_comment_id",
        foreignKey = @ForeignKey(name = "fk_tb_comments_root_comment_id__tb_comments")
    )
    private CommentEntity rootComment;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Builder
    private CommentEntity(
        UserEntity user,
        ArticleEntity article,
        CommentEntity parentComment,
        CommentEntity rootComment,
        int depth,
        String content
    ) {
        this.user = user;
        this.article = article;
        this.parentComment = parentComment;
        this.rootComment = rootComment;
        this.depth = depth;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void assignRootComment(CommentEntity rootComment) {
        this.rootComment = rootComment;
    }
}
