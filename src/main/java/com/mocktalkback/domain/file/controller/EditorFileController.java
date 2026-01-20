package com.mocktalkback.domain.file.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.service.EditorFileService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "EditorFile", description = "에디터 파일 업로드 API")
public class EditorFileController {

    private final EditorFileService editorFileService;

    @PostMapping(value = "/files/editor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "에디터 파일 업로드", description = "에디터에서 사용할 이미지/영상 파일을 업로드합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "업로드 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<FileResponse> uploadEditorFile(
        @RequestPart("file") MultipartFile file
    ) {
        return ApiEnvelope.ok(editorFileService.uploadEditorFile(file));
    }
}
