package com.mocktalkback.domain.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.comment.dto.CommentCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentFileCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentFileResponse;
import com.mocktalkback.domain.comment.dto.CommentReactionCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentReactionResponse;
import com.mocktalkback.domain.comment.dto.CommentResponse;
import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.entity.CommentFileEntity;
import com.mocktalkback.domain.comment.entity.CommentReactionEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface CommentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "articleId", source = "article.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "rootCommentId", source = "rootComment.id")
    CommentResponse toResponse(CommentEntity entity);

    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "commentId", source = "comment.id")
    CommentFileResponse toResponse(CommentFileEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "commentId", source = "comment.id")
    CommentReactionResponse toResponse(CommentReactionEntity entity);

    default CommentEntity toEntity(
        CommentCreateRequest request,
        UserEntity user,
        ArticleEntity article,
        CommentEntity parentComment,
        CommentEntity rootComment
    ) {
        return CommentEntity.builder()
            .user(user)
            .article(article)
            .parentComment(parentComment)
            .rootComment(rootComment)
            .depth(request.depth())
            .content(request.content())
            .build();
    }

    default CommentFileEntity toEntity(
        CommentFileCreateRequest request,
        FileEntity file,
        CommentEntity comment
    ) {
        return CommentFileEntity.builder()
            .file(file)
            .comment(comment)
            .build();
    }

    default CommentReactionEntity toEntity(
        CommentReactionCreateRequest request,
        UserEntity user,
        CommentEntity comment
    ) {
        return CommentReactionEntity.builder()
            .user(user)
            .comment(comment)
            .reactionType(request.reactionType())
            .build();
    }
}
