package com.mocktalkback.domain.file.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.file.upload.dto.UploadCompleteRequest;
import com.mocktalkback.domain.file.upload.dto.UploadCompleteResponse;
import com.mocktalkback.domain.file.upload.dto.UploadInitRequest;
import com.mocktalkback.domain.file.upload.dto.UploadInitResponse;
import com.mocktalkback.domain.file.upload.service.UploadSessionService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
@Tag(name = "UploadSession", description = "Presigned 업로드 세션 API")
public class UploadSessionController {

    private final UploadSessionService uploadSessionService;

    @PostMapping("/init")
    @Operation(summary = "업로드 세션 시작", description = "업로드 정책 검증 후 Presigned URL을 발급합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "발급 성공"),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<UploadInitResponse> init(
        @RequestBody @Valid UploadInitRequest request
    ) {
        return ApiEnvelope.ok(uploadSessionService.init(request));
    }

    @PostMapping("/complete")
    @Operation(summary = "업로드 완료 확정", description = "직접 업로드된 파일을 도메인 데이터와 연결합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "확정 성공"),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<UploadCompleteResponse> complete(
        @RequestBody @Valid UploadCompleteRequest request
    ) {
        return ApiEnvelope.ok(uploadSessionService.complete(request.uploadToken()));
    }

    @DeleteMapping("/{uploadToken}")
    @Operation(summary = "업로드 세션 취소", description = "업로드 세션과 임시 파일을 정리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "취소 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<Void> cancel(
        @PathVariable("uploadToken") String uploadToken
    ) {
        uploadSessionService.cancel(uploadToken);
        return ApiEnvelope.ok();
    }
}
