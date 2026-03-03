package com.mocktalkback.domain.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.moderation.type.ReportTargetType;

public interface CommentRepositoryCustom {
    Page<CommentEntity> findAdminBoardComments(
        Long boardId,
        Long authorId,
        Boolean reported,
        ReportTargetType targetType,
        Pageable pageable
    );
}
