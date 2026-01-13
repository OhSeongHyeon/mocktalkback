package com.mocktalkback.domain.file.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.file.dto.FileCreateRequest;
import com.mocktalkback.domain.file.dto.FileResponse;
import com.mocktalkback.domain.file.dto.FileUpdateRequest;
import com.mocktalkback.domain.file.service.FileService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping
    public ApiEnvelope<FileResponse> create(@RequestBody @Valid FileCreateRequest request) {
        return ApiEnvelope.ok(fileService.create(request));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<FileResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(fileService.findById(id));
    }

    @GetMapping
    public ApiEnvelope<List<FileResponse>> findAll() {
        return ApiEnvelope.ok(fileService.findAll());
    }

    @PutMapping("/{id}")
    public ApiEnvelope<FileResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid FileUpdateRequest request
    ) {
        return ApiEnvelope.ok(fileService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        fileService.delete(id);
        return ApiEnvelope.ok();
    }
}
