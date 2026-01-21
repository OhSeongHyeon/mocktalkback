package com.mocktalkback.domain.moderation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.moderation.dto.ReportCreateRequest;
import com.mocktalkback.domain.moderation.dto.ReportDetailResponse;
import com.mocktalkback.domain.moderation.service.ModerationService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Report", description = "신고 접수 API")
public class ReportController {

    private final ModerationService moderationService;

    @PostMapping("/reports")
    @Operation(summary = "신고 접수", description = "게시글/댓글/사용자/게시판을 신고합니다.")
    public ApiEnvelope<ReportDetailResponse> createReport(
        @RequestBody @Valid ReportCreateRequest request
    ) {
        return ApiEnvelope.ok(moderationService.createReport(request));
    }
}
