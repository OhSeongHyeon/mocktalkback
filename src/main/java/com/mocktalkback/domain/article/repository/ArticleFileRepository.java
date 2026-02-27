package com.mocktalkback.domain.article.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.article.entity.ArticleFileEntity;

public interface ArticleFileRepository extends JpaRepository<ArticleFileEntity, Long> {
    @EntityGraph(attributePaths = {"file"})
    List<ArticleFileEntity> findAllByArticleIdOrderByCreatedAtAsc(Long articleId);

    @EntityGraph(attributePaths = {"file"})
    Optional<ArticleFileEntity> findByArticleIdAndFileId(Long articleId, Long fileId);

    boolean existsByFileId(Long fileId);
}
