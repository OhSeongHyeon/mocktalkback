package com.mocktalkback.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleBookmarkEntity;

public interface ArticleBookmarkRepository extends JpaRepository<ArticleBookmarkEntity, Long> {
}
