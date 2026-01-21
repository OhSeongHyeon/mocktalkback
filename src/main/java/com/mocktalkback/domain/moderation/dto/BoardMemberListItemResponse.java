package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.board.entity.BoardMemberEntity;
import com.mocktalkback.domain.board.type.BoardRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시판 멤버 목록 응답")
public record BoardMemberListItemResponse(
    @Schema(description = "멤버 ID", example = "10")
    Long id,

    @Schema(description = "회원 ID", example = "3")
    Long userId,

    @Schema(description = "로그인 ID", example = "user01")
    String loginId,

    @Schema(description = "닉네임", example = "홍길동")
    String displayName,

    @Schema(description = "핸들", example = "hong")
    String handle,

    @Schema(description = "멤버 상태", example = "PENDING")
    BoardRole boardRole,

    @Schema(description = "승인자 ID", example = "1")
    Long grantedByUserId,

    @Schema(description = "승인자 닉네임", example = "관리자")
    String grantedByName,

    @Schema(description = "가입 신청일")
    Instant createdAt,

    @Schema(description = "상태 변경일")
    Instant updatedAt
) {
    public static BoardMemberListItemResponse from(BoardMemberEntity entity) {
        Long grantedById = entity.getGrantedByUser() != null ? entity.getGrantedByUser().getId() : null;
        String grantedByName = entity.getGrantedByUser() != null ? entity.getGrantedByUser().getDisplayName() : null;
        return new BoardMemberListItemResponse(
            entity.getId(),
            entity.getUser().getId(),
            entity.getUser().getLoginId(),
            entity.getUser().getDisplayName(),
            entity.getUser().getHandle(),
            entity.getBoardRole(),
            grantedById,
            grantedByName,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
