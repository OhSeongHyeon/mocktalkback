package com.mocktalkback.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "File update request")
public record FileUpdateRequest(
    @Schema(description = "File class id", example = "1")
    @NotNull
    @Positive
    Long fileClassId,

    @Schema(description = "File name", example = "photo.jpg")
    @NotBlank
    @Size(max = 255)
    String fileName,

    @Schema(description = "Storage key", example = "uploads/2024/01/photo.jpg")
    @NotBlank
    @Size(max = 1024)
    String storageKey,

    @Schema(description = "File size", example = "2048")
    @NotNull
    @Positive
    Long fileSize,

    @Schema(description = "MIME type", example = "image/jpeg")
    @NotBlank
    @Size(max = 128)
    String mimeType
) {
}
