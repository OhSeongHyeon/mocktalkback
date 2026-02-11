package com.mocktalkback.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 스냅샷 응답")
public record CommentSnapshotResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long articleId,

    @Schema(description = "댓글 동기화 버전", example = "42")
    long syncVersion,

    @Schema(description = "댓글 페이지 스냅샷")
    CommentPageResponse<CommentTreeResponse> page
) {
}
