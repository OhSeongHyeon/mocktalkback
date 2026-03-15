package com.mocktalkback.domain.content.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.content.dto.MarketOverviewResponse;
import com.mocktalkback.domain.content.dto.MarketSeriesResponse;
import com.mocktalkback.domain.content.service.ContentMarketService;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.domain.content.type.MarketSeriesPeriod;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents/market")
@Tag(name = "ContentMarket", description = "콘텐츠 시세 API")
public class ContentMarketController {

    private final ContentMarketService contentMarketService;

    @GetMapping("/overview")
    @Operation(summary = "환율/금값 요약 조회", description = "콘텐츠 화면에서 사용할 환율/금값 최신 요약 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class)))
    })
    public ApiEnvelope<MarketOverviewResponse> findOverview() {
        return ApiEnvelope.ok(contentMarketService.findOverview());
    }

    @GetMapping("/series")
    @Operation(summary = "환율/금값 시계열 조회", description = "선택한 종목의 기간별 시계열 데이터를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class)))
    })
    public ApiEnvelope<MarketSeriesResponse> findSeries(
        @Parameter(description = "종목 코드", example = "USD_KRW")
        @RequestParam(name = "instrument") MarketInstrumentCode instrumentCode,
        @Parameter(description = "조회 기간", example = "MONTH")
        @RequestParam(name = "period", defaultValue = "MONTH") MarketSeriesPeriod period,
        @Parameter(description = "직접 선택 시작일", example = "2026-03-01")
        @RequestParam(name = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "직접 선택 종료일", example = "2026-03-15")
        @RequestParam(name = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiEnvelope.ok(contentMarketService.findSeries(instrumentCode, period, startDate, endDate));
    }
}
