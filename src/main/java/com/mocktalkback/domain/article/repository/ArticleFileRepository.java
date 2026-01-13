package com.mocktalkback.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleFileEntity;

public interface ArticleFileRepository extends JpaRepository<ArticleFileEntity, Long> {
}
