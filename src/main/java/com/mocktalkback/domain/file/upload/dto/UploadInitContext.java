package com.mocktalkback.domain.file.upload.dto;

import com.mocktalkback.domain.file.upload.type.BoardImageUploadChannel;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 목적별 추가 컨텍스트")
public record UploadInitContext(
    @Schema(description = "게시판 ID(BOARD_IMAGE에서 필수)", example = "1")
    Long boardId,

    @Schema(description = "게시판 이미지 업로드 채널", example = "BOARD_OWNER")
    BoardImageUploadChannel channel,

    @Schema(description = "이미지 메타데이터 보존 여부", example = "false")
    Boolean preserveMetadata
) {
}
