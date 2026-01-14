package com.mocktalkback.domain.article.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.article.entity.ArticleEntity;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    Page<ArticleEntity> findByUserId(Long userId, Pageable pageable);
}
