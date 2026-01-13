package com.mocktalkback.domain.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.notification.dto.NotificationCreateRequest;
import com.mocktalkback.domain.notification.dto.NotificationResponse;
import com.mocktalkback.domain.notification.dto.NotificationUpdateRequest;
import com.mocktalkback.domain.notification.service.NotificationService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ApiEnvelope<NotificationResponse> create(@RequestBody @Valid NotificationCreateRequest request) {
        return ApiEnvelope.ok(notificationService.create(request));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<NotificationResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(notificationService.findById(id));
    }

    @GetMapping
    public ApiEnvelope<List<NotificationResponse>> findAll() {
        return ApiEnvelope.ok(notificationService.findAll());
    }

    @PutMapping("/{id}")
    public ApiEnvelope<NotificationResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid NotificationUpdateRequest request
    ) {
        return ApiEnvelope.ok(notificationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        notificationService.delete(id);
        return ApiEnvelope.ok();
    }
}
