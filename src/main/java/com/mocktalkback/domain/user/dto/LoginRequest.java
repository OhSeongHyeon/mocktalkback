package com.mocktalkback.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
    @Schema(description = "로그인 아이디", example = "user01")
    @NotBlank
    String loginId,

    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    @NotBlank
    String password,

    @Schema(description = "로그인 유지 여부", example = "false")
    boolean rememberMe
) {

}
