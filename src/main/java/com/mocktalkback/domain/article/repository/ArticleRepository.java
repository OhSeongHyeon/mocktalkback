package com.mocktalkback.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleEntity;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
}
