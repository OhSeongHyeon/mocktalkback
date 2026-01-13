package com.mocktalkback.domain.article.entity;

import com.mocktalkback.global.common.entity.BaseTimeEntity;
import com.mocktalkback.domain.file.entity.FileEntity;

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
    name = "tb_article_files",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_article_files_article_id_file_id",
            columnNames = {"article_id", "file_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_article_files_file_id", columnList = "file_id")
    }
)
public class ArticleFileEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_file_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_article_files_file_id__tb_files")
    )
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "article_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_article_files_article_id__tb_articles")
    )
    private ArticleEntity article;

    @Builder
    private ArticleFileEntity(FileEntity file, ArticleEntity article) {
        this.file = file;
        this.article = article;
    }
}
