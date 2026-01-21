package com.mocktalkback.domain.moderation.dto;

import java.time.Instant;

import com.mocktalkback.domain.user.entity.UserEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 사용자 목록 항목")
public record AdminUserListItemResponse(
    @Schema(description = "회원 번호", example = "1")
    Long id,

    @Schema(description = "로그인 ID", example = "admin")
    String loginId,

    @Schema(description = "이메일", example = "admin@example.com")
    String email,

    @Schema(description = "이름", example = "Admin")
    String userName,

    @Schema(description = "표시 이름", example = "관리자")
    String displayName,

    @Schema(description = "핸들", example = "admin")
    String handle,

    @Schema(description = "권한명", example = "ADMIN")
    String roleName,

    @Schema(description = "활성화 여부", example = "true")
    boolean enabled,

    @Schema(description = "잠금 여부", example = "false")
    boolean locked,

    @Schema(description = "생성일시")
    Instant createdAt,

    @Schema(description = "수정일시")
    Instant updatedAt
) {
    public static AdminUserListItemResponse from(UserEntity entity) {
        return new AdminUserListItemResponse(
            entity.getId(),
            entity.getLoginId(),
            entity.getEmail(),
            entity.getUserName(),
            entity.getDisplayName(),
            entity.getHandle(),
            entity.getRole().getRoleName(),
            entity.isEnabled(),
            entity.isLocked(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
