package com.mocktalkback.domain.comment.entity;

import com.mocktalkback.domain.common.entity.BaseTimeEntity;
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
    name = "tb_comment_files",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_comment_files_comment_id_file_id",
            columnNames = {"comment_id", "file_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_comment_files_file_id", columnList = "file_id")
    }
)
public class CommentFileEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_file_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_comment_files_file_id__tb_files")
    )
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "comment_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_comment_files_comment_id__tb_comments")
    )
    private CommentEntity comment;

    @Builder
    private CommentFileEntity(FileEntity file, CommentEntity comment) {
        this.file = file;
        this.comment = comment;
    }
}
