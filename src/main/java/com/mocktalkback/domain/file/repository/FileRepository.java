package com.mocktalkback.domain.file.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.file.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findAllByTempExpiresAtBeforeAndDeletedAtIsNull(Instant now);
}
