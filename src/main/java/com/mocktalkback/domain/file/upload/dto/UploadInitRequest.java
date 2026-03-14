package com.mocktalkback.domain.file.upload.dto;

import com.mocktalkback.domain.file.upload.type.UploadPurpose;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "업로드 세션 시작 요청")
public record UploadInitRequest(
    @NotNull
    @Schema(description = "업로드 목적", example = "EDITOR_IMAGE")
    UploadPurpose purpose,

    @NotBlank
    @Size(max = 255)
    @Schema(description = "원본 파일명", example = "photo.png")
    String originalFileName,

    @NotBlank
    @Size(max = 128)
    @Schema(description = "파일 MIME 타입", example = "image/png")
    String contentType,

    @Positive
    @Schema(description = "파일 크기(byte)", example = "1024")
    long fileSize,

    @Valid
    @Schema(description = "업로드 컨텍스트")
    UploadInitContext context
) {
}
