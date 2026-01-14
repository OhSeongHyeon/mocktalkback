package com.mocktalkback.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.file.entity.FileClassEntity;

import java.util.Optional;

public interface FileClassRepository extends JpaRepository<FileClassEntity, Long> {
    Optional<FileClassEntity> findByCode(String code);
}
