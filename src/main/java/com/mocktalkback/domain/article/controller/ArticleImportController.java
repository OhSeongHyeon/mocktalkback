package com.mocktalkback.domain.article.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.article.dto.ArticleImportExecuteResponse;
import com.mocktalkback.domain.article.dto.ArticleImportPreviewResponse;
import com.mocktalkback.domain.article.service.ArticleImportService;
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
@RequestMapping("/api/articles/imports")
@Tag(name = "ArticleImport", description = "게시글 Markdown 대량 임포트 API")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ArticleImportController {

    private final ArticleImportService articleImportService;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 대량 임포트 미리보기", description = "zip 파일을 분석해 게시글 생성 가능 여부를 미리 확인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "미리보기 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<ArticleImportPreviewResponse> preview(@RequestPart("file") MultipartFile file) {
        return ApiEnvelope.ok(articleImportService.preview(file));
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 대량 임포트 실행", description = "zip 파일을 기준으로 게시글을 일괄 생성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "실행 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<ArticleImportExecuteResponse> execute(@RequestPart("file") MultipartFile file) {
        return ApiEnvelope.ok(articleImportService.execute(file));
    }
}
