package com.mocktalkback.domain.user.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 32) String userName,
        @NotBlank @Size(max = 16) String displayName,
        @NotBlank @Size(max = 24) String handle
) {}
