package com.mocktalkback.domain.file.dto;

import java.time.Instant;

import com.mocktalkback.domain.file.type.MediaKind;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "File class response")
public record FileClassResponse(
    @Schema(description = "File class id", example = "1")
    Long id,

    @Schema(description = "File class code", example = "PROFILE_IMAGE")
    String code,

    @Schema(description = "File class name", example = "Profile image")
    String name,

    @Schema(description = "Description", example = "User profile image")
    String description,

    @Schema(description = "Media kind", example = "IMAGE")
    MediaKind mediaKind,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "Deleted at", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
