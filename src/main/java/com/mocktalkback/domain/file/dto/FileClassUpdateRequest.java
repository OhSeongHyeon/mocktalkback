package com.mocktalkback.domain.file.dto;

import com.mocktalkback.domain.file.type.MediaKind;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "File class update request")
public record FileClassUpdateRequest(
    @Schema(description = "File class name", example = "Profile image")
    @NotBlank
    @Size(max = 64)
    String name,

    @Schema(description = "Description", example = "User profile image")
    @Size(max = 255)
    String description,

    @Schema(description = "Media kind", example = "IMAGE")
    @NotNull
    MediaKind mediaKind
) {
}
