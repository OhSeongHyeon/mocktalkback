package com.mocktalkback.domain.article.entity;

import com.mocktalkback.domain.common.entity.BaseTimeEntity;
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
    name = "tb_article_bookmarks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_article_bookmarks_user_id_article_id",
            columnNames = {"user_id", "article_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_article_bookmarks_article_id", columnList = "article_id")
    }
)
public class ArticleBookmarkEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_bookmark_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_article_bookmarks_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "article_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_article_bookmarks_article_id__tb_articles")
    )
    private ArticleEntity article;

    @Builder
    private ArticleBookmarkEntity(UserEntity user, ArticleEntity article) {
        this.user = user;
        this.article = article;
    }
}
