package com.mocktalkback.domain.file.entity;

import com.mocktalkback.domain.file.type.FileVariantCode;
import com.mocktalkback.global.common.entity.SoftDeleteEntity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_file_variants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tb_file_variants_storage_key", columnNames = "storage_key"),
        @UniqueConstraint(name = "uq_tb_file_variants_file_id_variant_code", columnNames = {"file_id", "variant_code"})
    }
)
public class FileVariantEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "file_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_file_variants_file_id__tb_files")
    )
    private FileEntity file;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_code", nullable = false, length = 32)
    private FileVariantCode variantCode;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Builder
    private FileVariantEntity(
        FileEntity file,
        FileVariantCode variantCode,
        String storageKey,
        Long fileSize,
        String mimeType,
        Integer width,
        Integer height
    ) {
        this.file = file;
        this.variantCode = variantCode;
        this.storageKey = storageKey;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }
}
