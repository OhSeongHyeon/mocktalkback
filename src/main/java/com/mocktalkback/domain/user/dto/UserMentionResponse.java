package com.mocktalkback.domain.user.dto;

import com.mocktalkback.domain.file.dto.FileResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멘션 추천 응답")
public record UserMentionResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,
    @Schema(description = "핸들", example = "mocktalk")
    String handle,
    @Schema(description = "닉네임", example = "MockTalk")
    String displayName,
    @Schema(description = "프로필 이미지", nullable = true)
    FileResponse profileImage
) {}
