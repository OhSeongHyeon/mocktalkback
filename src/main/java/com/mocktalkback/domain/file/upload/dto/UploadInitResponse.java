package com.mocktalkback.domain.file.upload.dto;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 세션 시작 응답")
public record UploadInitResponse(
    @Schema(description = "업로드 토큰", example = "11c5f95c-ef99-4f36-a49c-c002e4834372")
    String uploadToken,

    @Schema(description = "파일 직접 업로드 URL", example = "/storage/mocktalk/uploads/...")
    String uploadUrl,

    @Schema(description = "업로드 메서드", example = "PUT")
    String method,

    @Schema(description = "업로드 시 필수 헤더")
    Map<String, String> headers,

    @Schema(description = "업로드 URL 만료 시각", example = "2026-03-05T12:00:00Z")
    Instant expiresAt
) {
}
