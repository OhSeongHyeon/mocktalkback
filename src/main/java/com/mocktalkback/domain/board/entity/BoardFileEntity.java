package com.mocktalkback.domain.board.entity;

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
    name = "tb_board_files",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_board_files_board_id_file_id",
            columnNames = {"board_id", "file_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_board_files_file_id", columnList = "file_id")
    }
)
public class BoardFileEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_file_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_board_files_file_id__tb_files")
    )
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "board_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_board_files_board_id__tb_boards")
    )
    private BoardEntity board;

    @Builder
    private BoardFileEntity(FileEntity file, BoardEntity board) {
        this.file = file;
        this.board = board;
    }
}
