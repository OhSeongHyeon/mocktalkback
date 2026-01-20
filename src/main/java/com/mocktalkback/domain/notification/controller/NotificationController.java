package com.mocktalkback.domain.notification.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.notification.dto.NotificationResponse;
import com.mocktalkback.domain.notification.service.NotificationService;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 단건 조회", description = "로그인 사용자 기준으로 알림 상세를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림 없음")
    })
    @GetMapping("/notifications/{id:\\d+}")
    public ApiEnvelope<NotificationResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(notificationService.findById(id));
    }

    @Operation(summary = "알림 목록 조회", description = "로그인 사용자 기준으로 알림 목록을 페이징 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/notifications")
    public ApiEnvelope<PageResponse<NotificationResponse>> findAll(
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기(최대 50)", example = "10")
        @RequestParam(name = "size", defaultValue = "10") int size,
        @Parameter(description = "읽음 여부 필터(true/false)", example = "false")
        @RequestParam(name = "read", required = false) Boolean read
    ) {
        return ApiEnvelope.ok(notificationService.findAll(page, size, read));
    }

    @Operation(summary = "알림 읽음 처리", description = "단일 알림을 읽음 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림 없음")
    })
    @PatchMapping("/notifications/{id:\\d+}/read")
    public ApiEnvelope<NotificationResponse> markRead(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(notificationService.markRead(id));
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "로그인 사용자의 모든 알림을 읽음 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PatchMapping("/notifications/read-all")
    public ApiEnvelope<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiEnvelope.ok();
    }

    @Operation(summary = "알림 삭제", description = "로그인 사용자의 알림을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림 없음")
    })
    @DeleteMapping("/notifications/{id:\\d+}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        notificationService.delete(id);
        return ApiEnvelope.ok();
    }

    @Operation(summary = "알림 전체 삭제", description = "로그인 사용자의 모든 알림을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/notifications")
    public ApiEnvelope<Void> deleteAll() {
        notificationService.deleteAll();
        return ApiEnvelope.ok();
    }
}
