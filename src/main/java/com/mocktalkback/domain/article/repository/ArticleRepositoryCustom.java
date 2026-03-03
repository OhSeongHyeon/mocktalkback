package com.mocktalkback.domain.article.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.moderation.type.ReportTargetType;

public interface ArticleRepositoryCustom {
    Page<ArticleEntity> findAdminBoardArticles(
        Long boardId,
        Long authorId,
        Boolean notice,
        Boolean reported,
        ReportTargetType targetType,
        Pageable pageable
    );
}
