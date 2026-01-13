package com.mocktalkback.domain.user.entity;

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
    name = "tb_user_files",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_user_files_user_id_file_id",
            columnNames = {"user_id", "file_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_user_files_file_id", columnList = "file_id")
    }
)
public class UserFileEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_file_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_user_files_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_user_files_file_id__tb_files")
    )
    private FileEntity file;

    @Builder
    private UserFileEntity(UserEntity user, FileEntity file) {
        this.user = user;
        this.file = file;
    }
}
