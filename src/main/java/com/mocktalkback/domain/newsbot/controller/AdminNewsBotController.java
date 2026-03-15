package com.mocktalkback.domain.newsbot.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobResponse;
import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobRunResponse;
import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobToggleRequest;
import com.mocktalkback.domain.newsbot.dto.AdminNewsBotJobUpsertRequest;
import com.mocktalkback.domain.newsbot.service.AdminNewsBotService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/news-bot/jobs")
@Tag(name = "AdminNewsBot", description = "뉴스봇 운영 API")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminNewsBotController {

    private final AdminNewsBotService adminNewsBotService;

    @GetMapping
    @Operation(summary = "뉴스봇 잡 목록", description = "백오피스에서 뉴스봇 잡 목록을 조회합니다.")
    public ApiEnvelope<List<AdminNewsBotJobResponse>> getJobs() {
        return ApiEnvelope.ok(adminNewsBotService.getJobs());
    }

    @PostMapping
    @Operation(summary = "뉴스봇 잡 생성", description = "새 뉴스봇 잡을 생성합니다.")
    public ApiEnvelope<AdminNewsBotJobResponse> createJob(
        @RequestBody @Valid AdminNewsBotJobUpsertRequest request
    ) {
        return ApiEnvelope.ok(adminNewsBotService.createJob(request));
    }

    @PutMapping("/{jobId:\\d+}")
    @Operation(summary = "뉴스봇 잡 수정", description = "기존 뉴스봇 잡을 수정합니다.")
    public ApiEnvelope<AdminNewsBotJobResponse> updateJob(
        @PathVariable("jobId") Long jobId,
        @RequestBody @Valid AdminNewsBotJobUpsertRequest request
    ) {
        return ApiEnvelope.ok(adminNewsBotService.updateJob(jobId, request));
    }

    @PatchMapping("/{jobId:\\d+}/enabled")
    @Operation(summary = "뉴스봇 잡 on/off", description = "뉴스봇 잡 활성화 상태를 변경합니다.")
    public ApiEnvelope<AdminNewsBotJobResponse> changeEnabled(
        @PathVariable("jobId") Long jobId,
        @RequestBody @Valid AdminNewsBotJobToggleRequest request
    ) {
        return ApiEnvelope.ok(adminNewsBotService.changeEnabled(jobId, request));
    }

    @PostMapping("/{jobId:\\d+}/run")
    @Operation(summary = "뉴스봇 잡 즉시 실행", description = "뉴스봇 잡을 즉시 1회 실행합니다.")
    public ApiEnvelope<AdminNewsBotJobRunResponse> runNow(@PathVariable("jobId") Long jobId) {
        return ApiEnvelope.ok(adminNewsBotService.runNow(jobId));
    }
}
