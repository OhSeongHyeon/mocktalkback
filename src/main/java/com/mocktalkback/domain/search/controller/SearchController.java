package com.mocktalkback.domain.search.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.search.dto.SearchResponse;
import com.mocktalkback.domain.search.service.SearchService;
import com.mocktalkback.domain.search.type.SearchType;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.type.SortOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Search", description = "통합 검색 API")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "통합 검색", description = "게시판/게시글/댓글/사용자를 검색합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "검색 결과 반환",
            content = @Content(schema = @Schema(implementation = SearchResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/search")
    public ApiEnvelope<SearchResponse> search(
        @Parameter(description = "검색어") @RequestParam("q") String keyword,
        @Parameter(description = "검색 유형") @RequestParam(value = "type", required = false) SearchType type,
        @Parameter(description = "정렬(최신순/과거순)") @RequestParam(value = "order", required = false) SortOrder order,
        @Parameter(description = "페이지(0부터 시작)") @RequestParam(value = "page", required = false) Integer page,
        @Parameter(description = "페이지 크기(최대 50)") @RequestParam(value = "size", required = false) Integer size,
        @Parameter(description = "게시판 슬러그 필터") @RequestParam(value = "boardSlug", required = false) String boardSlug
    ) {
        return ApiEnvelope.ok(searchService.search(keyword, type, order, page, size, boardSlug));
    }
}
