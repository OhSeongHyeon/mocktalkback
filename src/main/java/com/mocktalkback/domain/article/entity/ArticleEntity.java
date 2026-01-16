package com.mocktalkback.domain.article.entity;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.global.common.entity.SoftDeleteEntity;
import com.mocktalkback.domain.role.type.ContentVisibility;
import com.mocktalkback.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tb_articles")
public class ArticleEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "board_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_articles_board_id__tb_boards")
    )
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_articles_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "article_category_id",
        foreignKey = @ForeignKey(name = "fk_tb_articles_article_category_id__tb_article_categories")
    )
    private ArticleCategoryEntity category;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 24)
    private ContentVisibility visibility;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "hit", nullable = false)
    private long hit;

    @Column(name = "is_notice", nullable = false)
    private boolean notice;

    @Builder
    private ArticleEntity(
        BoardEntity board,
        UserEntity user,
        ArticleCategoryEntity category,
        ContentVisibility visibility,
        String title,
        String content,
        long hit,
        boolean notice
    ) {
        this.board = board;
        this.user = user;
        this.category = category;
        this.visibility = visibility;
        this.title = title;
        this.content = content;
        this.hit = hit;
        this.notice = notice;
    }

    public void update(
        ArticleCategoryEntity category,
        ContentVisibility visibility,
        String title,
        String content,
        boolean notice
    ) {
        this.category = category;
        this.visibility = visibility;
        this.title = title;
        this.content = content;
        this.notice = notice;
    }

    public void increaseHit() {
        this.hit += 1;
    }
}
