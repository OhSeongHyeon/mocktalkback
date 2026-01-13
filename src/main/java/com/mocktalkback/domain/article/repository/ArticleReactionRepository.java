package com.mocktalkback.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleReactionEntity;

public interface ArticleReactionRepository extends JpaRepository<ArticleReactionEntity, Long> {
}
