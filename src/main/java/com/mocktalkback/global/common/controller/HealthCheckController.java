package com.mocktalkback.global.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "헬스 체크 API")
public class HealthCheckController {

    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "정상 응답")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }

}
