package com.mocktalkback.domain.file.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "File response")
public record FileResponse(
    @Schema(description = "File id", example = "1")
    Long id,

    @Schema(description = "File class id", example = "1")
    Long fileClassId,

    @Schema(description = "File name", example = "photo.jpg")
    String fileName,

    @Schema(description = "Storage key", example = "uploads/2024/01/photo.jpg")
    String storageKey,

    @Schema(description = "File size", example = "1024")
    Long fileSize,

    @Schema(description = "MIME type", example = "image/jpeg")
    String mimeType,

    @Schema(description = "Created at", example = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Updated at", example = "2024-01-01T00:00:00Z")
    Instant updatedAt,

    @Schema(description = "Deleted at", example = "2024-01-01T00:00:00Z")
    Instant deletedAt
) {
}
