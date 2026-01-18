package com.mocktalkback.domain.comment.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 트리 응답")
public record CommentTreeResponse(
    @Schema(description = "댓글 ID", example = "1")
    Long id,

    @Schema(description = "작성자 ID", example = "1")
    Long userId,

    @Schema(description = "작성자 표시 이름", example = "mocktalk")
    String authorName,

    @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
    String content,

    @Schema(description = "깊이", example = "0")
    int depth,

    @Schema(description = "부모 댓글 ID", example = "10")
    Long parentCommentId,

    @Schema(description = "루트 댓글 ID", example = "1")
    Long rootCommentId,

    @Schema(description = "작성 시각")
    Instant createdAt,

    @Schema(description = "수정 시각")
    Instant updatedAt,

    @Schema(description = "삭제 시각")
    Instant deletedAt,

    @Schema(description = "하위 댓글")
    List<CommentTreeResponse> children
) {
}
