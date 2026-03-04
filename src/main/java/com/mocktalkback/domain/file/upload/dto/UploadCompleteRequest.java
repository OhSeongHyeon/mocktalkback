package com.mocktalkback.domain.file.upload.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "업로드 완료 확정 요청")
public record UploadCompleteRequest(
    @NotBlank
    @Size(max = 128)
    @Schema(description = "업로드 토큰", example = "11c5f95c-ef99-4f36-a49c-c002e4834372")
    String uploadToken
) {
}
