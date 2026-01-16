package com.mocktalkback.domain.article.entity;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.global.common.entity.BaseTimeEntity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_article_categories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_article_categories_board_id_category_name",
            columnNames = {"board_id", "category_name"}
        )
    }
)
public class ArticleCategoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_category_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "board_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_article_categories_board_id__tb_boards")
    )
    private BoardEntity board;

    @Column(name = "category_name", nullable = false, length = 48)
    private String categoryName;

    @Builder
    private ArticleCategoryEntity(BoardEntity board, String categoryName) {
        this.board = board;
        this.categoryName = categoryName;
    }

    public void updateName(String categoryName) {
        this.categoryName = categoryName;
    }
}
