package com.mocktalkback.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(
        @NotBlank
        @Size(max = 128)
        String loginId,

        @NotBlank
        @Email
        @Size(max = 128)
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,
                
        @NotBlank
        @Size(min = 8, max = 64)
        String confirmPassword,

        @Size(max = 32)
        String userName,

        @Size(max = 16)
        String displayName,

        @Size(max = 24)
        String handle
) {

}
