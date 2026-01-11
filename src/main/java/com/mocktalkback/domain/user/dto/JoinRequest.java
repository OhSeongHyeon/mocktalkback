package com.mocktalkback.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record JoinRequest(
        @Schema(description = "로그인 아이디", example = "user01")
        @NotBlank
        @Size(max = 128)
        String loginId,

        @Schema(description = "이메일", example = "user01@example.com")
        @NotBlank
        @Email
        @Size(max = 128)
        String email,

        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        @NotBlank
        @Size(min = 8, max = 64)
        String password,
                
        @Schema(description = "비밀번호 확인", example = "P@ssw0rd!")
        @NotBlank
        @Size(min = 8, max = 64)
        String confirmPassword,

        @Schema(description = "사용자명", example = "mocktalk_user")
        @Size(max = 32)
        String userName,

        @Schema(description = "표시명", example = "MockTalk")
        @Size(max = 16)
        String displayName,

        @Schema(description = "핸들", example = "mocktalk01")
        @Size(max = 24)
        String handle
) {

}
