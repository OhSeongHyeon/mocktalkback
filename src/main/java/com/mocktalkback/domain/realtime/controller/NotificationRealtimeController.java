package com.mocktalkback.domain.realtime.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mocktalkback.domain.realtime.dto.NotificationPresenceUpdateRequest;
import com.mocktalkback.domain.realtime.service.NotificationPresenceService;
import com.mocktalkback.domain.realtime.service.NotificationRealtimeSseService;
import com.mocktalkback.global.auth.CurrentUserService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/realtime/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Realtime", description = "알림 실시간 이벤트 스트림 API")
public class NotificationRealtimeController {

    private final NotificationRealtimeSseService notificationRealtimeSseService;
    private final NotificationPresenceService notificationPresenceService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "알림 실시간 이벤트 구독", description = "로그인 사용자 기준 실시간 알림 이벤트 스트림을 구독합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "구독 성공", content = @Content(schema = @Schema(implementation = SseEmitter.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "마지막 수신 이벤트 아이디", example = "event-123")
            @RequestHeader(value = "Last-Event-ID", required = false)
            String lastEventId
    ) {
        Long userId = currentUserService.getUserId();
        return notificationRealtimeSseService.subscribe(userId, lastEventId);
    }

    @Operation(summary = "알림 presence 업데이트", description = "현재 화면/알림 패널 상태를 heartbeat와 함께 업데이트합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PutMapping("/presence")
    public ApiEnvelope<Void> updatePresence(@Valid @RequestBody NotificationPresenceUpdateRequest request) {
        Long userId = currentUserService.getUserId();
        notificationPresenceService.upsert(userId, request);
        return ApiEnvelope.ok();
    }

    @Operation(summary = "알림 presence 제거", description = "탭 종료/로그아웃 시 해당 presence 세션을 제거합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "제거 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/presence/{sessionId}")
    public ApiEnvelope<Void> removePresence(@PathVariable("sessionId") String sessionId) {
        Long userId = currentUserService.getUserId();
        notificationPresenceService.remove(userId, sessionId);
        return ApiEnvelope.ok();
    }
}
