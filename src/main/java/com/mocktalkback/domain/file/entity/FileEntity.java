package com.mocktalkback.domain.file.entity;

import com.mocktalkback.global.common.entity.SoftDeleteEntity;

import java.time.Instant;

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
    name = "tb_files",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tb_files_storage_key", columnNames = "storage_key")
    }
)
public class FileEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_class_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_files_file_class_id__tb_file_classes")
    )
    private FileClassEntity fileClass;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "metadata_preserved", nullable = false)
    private boolean metadataPreserved;

    @Column(name = "temp_expires_at")
    private Instant tempExpiresAt;

    @Builder
    private FileEntity(
        FileClassEntity fileClass,
        String fileName,
        String storageKey,
        Long fileSize,
        String mimeType,
        boolean metadataPreserved,
        Instant tempExpiresAt
    ) {
        this.fileClass = fileClass;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.metadataPreserved = metadataPreserved;
        this.tempExpiresAt = tempExpiresAt;
    }

    public void update(
        FileClassEntity fileClass,
        String fileName,
        String storageKey,
        Long fileSize,
        String mimeType,
        boolean metadataPreserved,
        Instant tempExpiresAt
    ) {
        this.fileClass = fileClass;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.metadataPreserved = metadataPreserved;
        this.tempExpiresAt = tempExpiresAt;
    }

    public void markTemporary(Instant expiresAt) {
        this.tempExpiresAt = expiresAt;
    }

    public void clearTemporary() {
        this.tempExpiresAt = null;
    }

    public boolean isTemporary() {
        return tempExpiresAt != null;
    }
}
