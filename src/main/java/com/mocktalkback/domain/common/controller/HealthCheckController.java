package com.mocktalkback.domain.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
@RestController
@RequestMapping("")
public class HealthCheckController {

    @PostMapping("/health")
    public ResponseEntity<ApiResponse<Void>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<Void>> authCheck() {
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
