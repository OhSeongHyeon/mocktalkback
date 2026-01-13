package com.mocktalkback.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategoryEntity, Long> {
}
