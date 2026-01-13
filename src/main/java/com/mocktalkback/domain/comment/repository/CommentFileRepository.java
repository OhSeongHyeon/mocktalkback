package com.mocktalkback.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.comment.entity.CommentFileEntity;

public interface CommentFileRepository extends JpaRepository<CommentFileEntity, Long> {
}
