package com.mocktalkback.domain.file.entity;

import com.mocktalkback.domain.common.entity.SoftDeleteEntity;
import com.mocktalkback.domain.file.type.MediaKind;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_file_classes")
public class FileClassEntity extends SoftDeleteEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_class_id", nullable = false)
    private Long id;

    @Column(name = "file_class_code", nullable = false, length = 32)
    private String code; // PROFILE_IMAGE, ARTICLE_ATTACHMENT ...

    @Column(name = "file_class_name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_kind", nullable = false, length = 16)
    private MediaKind mediaKind;

    @Builder
    public FileClassEntity(
        String code,
        String name,
        String description,
        MediaKind mediaKind
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.mediaKind = mediaKind;
    }

}

