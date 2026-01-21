package com.mocktalkback.domain.moderation.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.mocktalkback.domain.moderation.dto.AdminAuditLogResponse;
import com.mocktalkback.domain.moderation.dto.ReportDetailResponse;
import com.mocktalkback.domain.moderation.dto.ReportListItemResponse;
import com.mocktalkback.domain.moderation.dto.ReportProcessRequest;
import com.mocktalkback.domain.moderation.dto.SanctionCreateRequest;
import com.mocktalkback.domain.moderation.dto.SanctionResponse;
import com.mocktalkback.domain.moderation.dto.SanctionRevokeRequest;
import com.mocktalkback.domain.moderation.service.ModerationService;
import com.mocktalkback.domain.moderation.type.AdminActionType;
import com.mocktalkback.domain.moderation.type.AdminTargetType;
import com.mocktalkback.domain.moderation.type.ReportStatus;
import com.mocktalkback.domain.moderation.type.SanctionScopeType;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;
import com.mocktalkback.global.common.util.RequestMetadataResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "AdminModeration", description = "사이트 관리자 신고/제재/감사 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final ModerationService moderationService;

    @GetMapping("/admin/reports")
    @Operation(summary = "신고 목록", description = "사이트 관리자용 신고 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<ReportListItemResponse>> getReports(
        @RequestParam(name = "status", required = false) ReportStatus status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(moderationService.getAdminReports(status, page, size));
    }

    @GetMapping("/admin/reports/{id:\\d+}")
    @Operation(summary = "신고 상세", description = "신고 상세 정보를 조회합니다.")
    public ApiEnvelope<ReportDetailResponse> getReport(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(moderationService.getAdminReport(id));
    }

    @PutMapping("/admin/reports/{id:\\d+}")
    @Operation(summary = "신고 처리", description = "신고 상태를 처리합니다.")
    public ApiEnvelope<ReportDetailResponse> processReport(
        @PathVariable("id") Long id,
        @RequestBody @Valid ReportProcessRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestMetadataResolver.resolveClientIp(httpRequest);
        String userAgent = RequestMetadataResolver.resolveUserAgent(httpRequest);
        return ApiEnvelope.ok(moderationService.processAdminReport(id, request, ipAddress, userAgent));
    }

    @GetMapping("/admin/sanctions")
    @Operation(summary = "제재 목록", description = "제재 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<SanctionResponse>> getSanctions(
        @RequestParam(name = "scopeType", required = false) SanctionScopeType scopeType,
        @RequestParam(name = "boardId", required = false) Long boardId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(moderationService.getAdminSanctions(scopeType, boardId, page, size));
    }

    @PostMapping("/admin/sanctions")
    @Operation(summary = "제재 등록", description = "제재를 등록합니다.")
    public ApiEnvelope<SanctionResponse> createSanction(
        @RequestBody @Valid SanctionCreateRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestMetadataResolver.resolveClientIp(httpRequest);
        String userAgent = RequestMetadataResolver.resolveUserAgent(httpRequest);
        return ApiEnvelope.ok(moderationService.createAdminSanction(request, ipAddress, userAgent));
    }

    @PostMapping("/admin/sanctions/{id:\\d+}/revoke")
    @Operation(summary = "제재 해제", description = "제재를 해제합니다.")
    public ApiEnvelope<SanctionResponse> revokeSanction(
        @PathVariable("id") Long id,
        @RequestBody @Valid SanctionRevokeRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = RequestMetadataResolver.resolveClientIp(httpRequest);
        String userAgent = RequestMetadataResolver.resolveUserAgent(httpRequest);
        return ApiEnvelope.ok(moderationService.revokeAdminSanction(id, request, ipAddress, userAgent));
    }

    @GetMapping("/admin/audit-logs")
    @Operation(summary = "운영 로그 목록", description = "운영 로그를 조회합니다.")
    public ApiEnvelope<PageResponse<AdminAuditLogResponse>> getAuditLogs(
        @RequestParam(name = "actionType", required = false) AdminActionType actionType,
        @RequestParam(name = "actorUserId", required = false) Long actorUserId,
        @RequestParam(name = "targetType", required = false) AdminTargetType targetType,
        @RequestParam(name = "targetId", required = false) Long targetId,
        @RequestParam(name = "fromAt", required = false) Instant fromAt,
        @RequestParam(name = "toAt", required = false) Instant toAt,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(
            moderationService.getAdminAuditLogs(
                actionType,
                actorUserId,
                targetType,
                targetId,
                fromAt,
                toAt,
                page,
                size
            )
        );
    }
}
