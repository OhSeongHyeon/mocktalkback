package com.mocktalkback.domain.article.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mocktalkback.domain.article.dto.ArticleBookmarkCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleBookmarkResponse;
import com.mocktalkback.domain.article.dto.ArticleCategoryCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleCategoryResponse;
import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleFileCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleFileResponse;
import com.mocktalkback.domain.article.dto.ArticleReactionCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleReactionResponse;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.entity.ArticleBookmarkEntity;
import com.mocktalkback.domain.article.entity.ArticleCategoryEntity;
import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.ArticleFileEntity;
import com.mocktalkback.domain.article.entity.ArticleReactionEntity;
import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface ArticleMapper {

    @Mapping(target = "boardId", source = "board.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "categoryId", source = "category.id")
    ArticleResponse toResponse(ArticleEntity entity);

    @Mapping(target = "boardId", source = "board.id")
    ArticleCategoryResponse toResponse(ArticleCategoryEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "articleId", source = "article.id")
    ArticleBookmarkResponse toResponse(ArticleBookmarkEntity entity);

    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "articleId", source = "article.id")
    ArticleFileResponse toResponse(ArticleFileEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "articleId", source = "article.id")
    ArticleReactionResponse toResponse(ArticleReactionEntity entity);

    default ArticleEntity toEntity(
        ArticleCreateRequest request,
        BoardEntity board,
        UserEntity user,
        ArticleCategoryEntity category
    ) {
        return ArticleEntity.builder()
            .board(board)
            .user(user)
            .category(category)
            .visibility(request.visibility())
            .title(request.title())
            .content(request.content())
            .hit(0L)
            .notice(request.notice())
            .build();
    }

    default ArticleCategoryEntity toEntity(
        ArticleCategoryCreateRequest request,
        BoardEntity board
    ) {
        return ArticleCategoryEntity.builder()
            .board(board)
            .categoryName(request.categoryName())
            .build();
    }

    default ArticleBookmarkEntity toEntity(
        ArticleBookmarkCreateRequest request,
        UserEntity user,
        ArticleEntity article
    ) {
        return ArticleBookmarkEntity.builder()
            .user(user)
            .article(article)
            .build();
    }

    default ArticleFileEntity toEntity(
        ArticleFileCreateRequest request,
        FileEntity file,
        ArticleEntity article
    ) {
        return ArticleFileEntity.builder()
            .file(file)
            .article(article)
            .build();
    }

    default ArticleReactionEntity toEntity(
        ArticleReactionCreateRequest request,
        UserEntity user,
        ArticleEntity article
    ) {
        return ArticleReactionEntity.builder()
            .user(user)
            .article(article)
            .reactionType(request.reactionType())
            .build();
    }
}
