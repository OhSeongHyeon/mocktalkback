package com.mocktalkback.domain.realtime.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mocktalkback.domain.realtime.service.BoardRealtimeSseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/realtime/boards")
@RequiredArgsConstructor
@Tag(name = "Realtime", description = "게시판 실시간 이벤트 스트림 API")
public class BoardRealtimeController {

    private final BoardRealtimeSseService boardRealtimeSseService;

    @Operation(summary = "게시판 실시간 이벤트 구독", description = "게시판 단위 실시간 이벤트 스트림을 구독합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "구독 성공", content = @Content(schema = @Schema(implementation = SseEmitter.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류")
    })
    @GetMapping(value = "/{boardId:\\d+}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable("boardId")
            Long boardId,
            @Parameter(description = "마지막 수신 이벤트 아이디", example = "event-123")
            @RequestHeader(value = "Last-Event-ID", required = false)
            String lastEventId
    ) {
        return boardRealtimeSseService.subscribe(boardId, lastEventId);
    }
}
