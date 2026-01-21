package com.mocktalkback.domain.moderation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.mocktalkback.domain.moderation.dto.ReportDetailResponse;
import com.mocktalkback.domain.moderation.dto.ReportListItemResponse;
import com.mocktalkback.domain.moderation.dto.ReportProcessRequest;
import com.mocktalkback.domain.moderation.dto.SanctionCreateRequest;
import com.mocktalkback.domain.moderation.dto.SanctionResponse;
import com.mocktalkback.domain.moderation.dto.SanctionRevokeRequest;
import com.mocktalkback.domain.moderation.service.ModerationService;
import com.mocktalkback.domain.moderation.type.ReportStatus;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;
import com.mocktalkback.global.common.util.RequestMetadataResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/{boardId:\\d+}/admin")
@Tag(name = "BoardModeration", description = "커뮤니티 관리자 신고/제재 API")
public class BoardModerationController {

    private final ModerationService moderationService;

    @GetMapping("/reports")
    @Operation(summary = "게시판 신고 목록", description = "게시판 범위의 신고 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<ReportListItemResponse>> getReports(
        @PathVariable("boardId") Long boardId,
        @RequestParam(name = "status", required = false) ReportStatus status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(moderationService.getBoardReports(boardId, status, page, size));
    }

    @GetMapping("/reports/{id:\\d+}")
    @Operation(summary = "게시판 신고 상세", description = "게시판 신고 상세 정보를 조회합니다.")
    public ApiEnvelope<ReportDetailResponse> getReport(
        @PathVariable("boardId") Long boardId,
        @PathVariable("id") Long id
    ) {
        return ApiEnvelope.ok(moderationService.getBoardReport(boardId, id));
    }

    @PutMapping("/reports/{id:\\d+}")
    @Operation(summary = "게시판 신고 처리", description = "게시판 신고 상태를 처리합니다.")
    public ApiEnvelope<ReportDetailResponse> processReport(
        @PathVariable("boardId") Long boardId,
        @PathVariable("id") Long id,
        @RequestBody @Valid ReportProcessRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestMetadataResolver.resolveClientIp(httpRequest);
        String userAgent = RequestMetadataResolver.resolveUserAgent(httpRequest);
        return ApiEnvelope.ok(moderationService.processBoardReport(boardId, id, request, ipAddress, userAgent));
    }

    @GetMapping("/sanctions")
    @Operation(summary = "게시판 제재 목록", description = "게시판 범위 제재 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<SanctionResponse>> getSanctions(
        @PathVariable("boardId") Long boardId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(moderationService.getBoardSanctions(boardId, page, size));
    }

    @PostMapping("/sanctions")
    @Operation(summary = "게시판 제재 등록", description = "게시판 범위 제재를 등록합니다.")
    public ApiEnvelope<SanctionResponse> createSanction(
        @PathVariable("boardId") Long boardId,
        @RequestBody @Valid SanctionCreateRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestMetadataResolver.resolveClientIp(httpRequest);
        String userAgent = RequestMetadataResolver.resolveUserAgent(httpRequest);
        return ApiEnvelope.ok(moderationService.createBoardSanction(boardId, request, ipAddress, userAgent));
    }

    @PostMapping("/sanctions/{id:\\d+}/revoke")
    @Operation(summary = "게시판 제재 해제", description = "게시판 제재를 해제합니다.")
    public ApiEnvelope<SanctionResponse> revokeSanction(
        @PathVariable("boardId") Long boardId,
        @PathVariable("id") Long id,
        @RequestBody @Valid SanctionRevokeRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestMetadataResolver.resolveClientIp(httpRequest);
        String userAgent = RequestMetadataResolver.resolveUserAgent(httpRequest);
        return ApiEnvelope.ok(moderationService.revokeBoardSanction(boardId, id, request, ipAddress, userAgent));
    }
}
