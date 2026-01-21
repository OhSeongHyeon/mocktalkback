package com.mocktalkback.domain.user.dto;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "마이페이지 프로필 수정 요청")
@Getter
@Setter
public class UserProfileUpdateRequest {

    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    @Size(max = 32)
    private String userName;

    @Schema(description = "이메일", example = "user01@example.com")
    @NotBlank
    @Email
    @Size(max = 128)
    private String email;

    @Schema(description = "닉네임", example = "MockTalk")
    @Size(max = 16)
    private String displayName;

    @Schema(description = "핸들", example = "handle1234")
    @NotBlank
    @Size(max = 24)
    private String handle;

    @Schema(description = "새 비밀번호(비워두면 변경하지 않음)", example = "P@ssw0rd!")
    @Size(max = 64)
    private String password;

    @Schema(description = "프로필 이미지 파일", type = "string", format = "binary")
    private MultipartFile profileImage;

    @Schema(description = "프로필 이미지 메타데이터 보존 여부", example = "false")
    private boolean preserveMetadata;
}
