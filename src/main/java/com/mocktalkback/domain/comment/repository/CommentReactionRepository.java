package com.mocktalkback.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.comment.entity.CommentReactionEntity;

public interface CommentReactionRepository extends JpaRepository<CommentReactionEntity, Long> {
}
