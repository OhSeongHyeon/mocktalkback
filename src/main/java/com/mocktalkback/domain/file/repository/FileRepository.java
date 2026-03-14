package com.mocktalkback.domain.file.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.file.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    @EntityGraph(attributePaths = {"fileClass"})
    Optional<FileEntity> findByIdAndDeletedAtIsNull(Long fileId);

    List<FileEntity> findAllByTempExpiresAtBeforeAndDeletedAtIsNull(Instant now);
    boolean existsByStorageKeyAndDeletedAtIsNull(String storageKey);
}
