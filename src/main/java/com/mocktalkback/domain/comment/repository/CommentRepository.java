package com.mocktalkback.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.comment.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
}
