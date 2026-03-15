package com.mocktalkback.domain.content.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.content.dto.AdminMarketImportResponse;
import com.mocktalkback.domain.content.dto.AdminMarketRefreshResponse;
import com.mocktalkback.domain.content.service.AdminContentMarketService;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
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
@RequestMapping("/api/admin/contents/market")
@Tag(name = "AdminContentMarket", description = "콘텐츠 시세 운영 API")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminContentMarketController {

    private final AdminContentMarketService adminContentMarketService;

    @PostMapping("/refresh")
    @Operation(summary = "콘텐츠 시세 즉시 최신화", description = "운영자가 현재 시세를 즉시 다시 수집합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "실행 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<AdminMarketRefreshResponse> refreshNow() {
        return ApiEnvelope.ok(adminContentMarketService.refreshNow());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "콘텐츠 시세 파일 임포트", description = "CSV/XLSX 파일을 업로드해 과거 시세 데이터를 반영합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "임포트 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<AdminMarketImportResponse> importSnapshots(
        @RequestPart("file") MultipartFile file,
        @RequestParam(name = "instrument", required = false) MarketInstrumentCode instrumentCode
    ) {
        return ApiEnvelope.ok(adminContentMarketService.importSnapshots(file, instrumentCode));
    }
}
