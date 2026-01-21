package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.article.entity.ArticleEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 관리자 게시글 목록 응답")
public record BoardAdminArticleItemResponse(
    @Schema(description = "게시글 ID", example = "12")
    Long id,

    @Schema(description = "작성자 ID", example = "3")
    Long userId,

    @Schema(description = "작성자 닉네임", example = "홍길동")
    String authorName,

    @Schema(description = "제목", example = "공지사항입니다.")
    String title,

    @Schema(description = "공지 여부", example = "true")
    boolean notice,

    @Schema(description = "신고 여부", example = "false")
    boolean reported,

    @Schema(description = "작성일")
    Instant createdAt,

    @Schema(description = "삭제일")
    Instant deletedAt
) {
    public static BoardAdminArticleItemResponse from(ArticleEntity entity, boolean reported) {
        return new BoardAdminArticleItemResponse(
            entity.getId(),
            entity.getUser().getId(),
            entity.getUser().getDisplayName(),
            entity.getTitle(),
            entity.isNotice(),
            reported,
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
