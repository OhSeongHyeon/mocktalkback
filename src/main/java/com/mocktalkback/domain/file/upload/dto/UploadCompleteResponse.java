package com.mocktalkback.domain.file.upload.dto;

import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.upload.type.UploadPurpose;
import com.mocktalkback.domain.user.dto.UserProfileResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 완료 확정 응답")
public record UploadCompleteResponse(
    @Schema(description = "업로드 목적", example = "EDITOR_IMAGE")
    UploadPurpose purpose,

    @Schema(description = "파일 도메인 응답(에디터/첨부)")
    FileResponse file,

    @Schema(description = "게시판 도메인 응답(게시판 이미지)")
    BoardResponse board,

    @Schema(description = "사용자 도메인 응답(프로필 이미지)")
    UserProfileResponse userProfile
) {
}
