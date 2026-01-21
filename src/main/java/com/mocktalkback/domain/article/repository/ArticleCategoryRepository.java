package com.mocktalkback.domain.article.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategoryEntity, Long> {
    List<ArticleCategoryEntity> findAllByBoardIdOrderByCategoryNameAsc(Long boardId);

    boolean existsByBoardIdAndCategoryNameIgnoreCase(Long boardId, String categoryName);
}
