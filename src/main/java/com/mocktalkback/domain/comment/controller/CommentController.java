package com.mocktalkback.domain.comment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.comment.dto.CommentCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentResponse;
import com.mocktalkback.domain.comment.dto.CommentUpdateRequest;
import com.mocktalkback.domain.comment.service.CommentService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ApiEnvelope<CommentResponse> create(@RequestBody @Valid CommentCreateRequest request) {
        return ApiEnvelope.ok(commentService.create(request));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<CommentResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(commentService.findById(id));
    }

    @GetMapping
    public ApiEnvelope<List<CommentResponse>> findAll() {
        return ApiEnvelope.ok(commentService.findAll());
    }

    @PutMapping("/{id}")
    public ApiEnvelope<CommentResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid CommentUpdateRequest request
    ) {
        return ApiEnvelope.ok(commentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        commentService.delete(id);
        return ApiEnvelope.ok();
    }
}
