package com.mocktalkback.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.file.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}
