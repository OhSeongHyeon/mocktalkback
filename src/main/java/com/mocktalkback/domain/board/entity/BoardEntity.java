package com.mocktalkback.domain.board.entity;

import com.mocktalkback.global.common.entity.SoftDeleteEntity;
import com.mocktalkback.domain.board.type.BoardArticleWritePolicy;
import com.mocktalkback.domain.board.type.BoardVisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    name = "tb_boards",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tb_boards_board_name", columnNames = "board_name"),
        @UniqueConstraint(name = "uq_tb_boards_slug", columnNames = "slug")
    }
)
public class BoardEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id", nullable = false)
    private Long id;

    @Column(name = "board_name", nullable = false, length = 255)
    private String boardName;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    private BoardVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "article_write_policy", nullable = false, length = 32)
    private BoardArticleWritePolicy articleWritePolicy;

    @Builder
    private BoardEntity(
        String boardName,
        String slug,
        String description,
        BoardVisibility visibility,
        BoardArticleWritePolicy articleWritePolicy
    ) {
        this.boardName = boardName;
        this.slug = slug;
        this.description = description;
        this.visibility = visibility;
        this.articleWritePolicy = articleWritePolicy == null
            ? BoardArticleWritePolicy.ALL_AUTHENTICATED
            : articleWritePolicy;
    }

    public void update(
        String boardName,
        String slug,
        String description,
        BoardVisibility visibility
    ) {
        update(boardName, slug, description, visibility, this.articleWritePolicy);
    }

    public void update(
        String boardName,
        String slug,
        String description,
        BoardVisibility visibility,
        BoardArticleWritePolicy articleWritePolicy
    ) {
        this.boardName = boardName;
        this.slug = slug;
        this.description = description;
        this.visibility = visibility;
        this.articleWritePolicy = articleWritePolicy == null
            ? BoardArticleWritePolicy.ALL_AUTHENTICATED
            : articleWritePolicy;
    }
}
