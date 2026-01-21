package com.mocktalkback.domain.file.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.file.entity.FileVariantEntity;
import com.mocktalkback.domain.file.type.FileVariantCode;

public interface FileVariantRepository extends JpaRepository<FileVariantEntity, Long> {

    Optional<FileVariantEntity> findByFileIdAndVariantCodeAndDeletedAtIsNull(Long fileId, FileVariantCode variantCode);

    List<FileVariantEntity> findAllByFileIdAndDeletedAtIsNull(Long fileId);
}
