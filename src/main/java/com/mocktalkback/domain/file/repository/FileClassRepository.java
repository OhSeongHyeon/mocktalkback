package com.mocktalkback.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.file.entity.FileClassEntity;

public interface FileClassRepository extends JpaRepository<FileClassEntity, Long> {
}
