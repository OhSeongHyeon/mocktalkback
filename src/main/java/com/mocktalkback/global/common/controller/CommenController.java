package com.mocktalkback.global.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.global.common.dto.ApiEnvelope;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommenController {

    @GetMapping("/health")
    public ApiEnvelope<Void> healthChk() {
        return ApiEnvelope.ok();
    }


}
