package com.mocktalkback.domain.user.dto;

import com.mocktalkback.domain.file.dto.FileResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 프로필 응답")
public record UserProfileResponse(
    @Schema(description = "사용자 id", example = "1")
    Long userId,

    @Schema(description = "로그인 아이디", example = "user01")
    String loginId,

    @Schema(description = "이메일", example = "user01@example.com")
    String email,

    @Schema(description = "이름", example = "홍길동")
    String userName,

    @Schema(description = "닉네임", example = "MockTalk")
    String displayName,

    @Schema(description = "핸들", example = "handle1234")
    String handle,

    @Schema(description = "포인트", example = "0")
    int userPoint,

    @Schema(description = "프로필 이미지")
    FileResponse profileImage
) {
}
