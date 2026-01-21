package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.comment.entity.CommentEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 관리자 댓글 목록 응답")
public record BoardAdminCommentItemResponse(
    @Schema(description = "댓글 ID", example = "22")
    Long id,

    @Schema(description = "게시글 ID", example = "10")
    Long articleId,

    @Schema(description = "게시글 제목", example = "공지사항")
    String articleTitle,

    @Schema(description = "작성자 ID", example = "3")
    Long userId,

    @Schema(description = "작성자 닉네임", example = "홍길동")
    String authorName,

    @Schema(description = "내용 요약", example = "댓글 내용 일부")
    String content,

    @Schema(description = "뎁스", example = "0")
    int depth,

    @Schema(description = "신고 여부", example = "false")
    boolean reported,

    @Schema(description = "작성일")
    Instant createdAt,

    @Schema(description = "삭제일")
    Instant deletedAt
) {
    public static BoardAdminCommentItemResponse from(CommentEntity entity, boolean reported, String contentPreview) {
        return new BoardAdminCommentItemResponse(
            entity.getId(),
            entity.getArticle().getId(),
            entity.getArticle().getTitle(),
            entity.getUser().getId(),
            entity.getUser().getDisplayName(),
            contentPreview,
            entity.getDepth(),
            reported,
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
