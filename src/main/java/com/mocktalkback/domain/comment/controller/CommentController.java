package com.mocktalkback.domain.comment.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.comment.dto.CommentCreateRequest;
import com.mocktalkback.domain.comment.dto.CommentPageResponse;
import com.mocktalkback.domain.comment.dto.CommentReactionSummaryResponse;
import com.mocktalkback.domain.comment.dto.CommentReactionToggleRequest;
import com.mocktalkback.domain.comment.dto.CommentTreeResponse;
import com.mocktalkback.domain.comment.dto.CommentUpdateRequest;
import com.mocktalkback.domain.comment.service.CommentService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/articles/{articleId}/comments")
    public ApiEnvelope<CommentPageResponse<CommentTreeResponse>> getArticleComments(
        @PathVariable("articleId") Long articleId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(commentService.getArticleComments(articleId, page, size));
    }

    @PostMapping("/articles/{articleId}/comments")
    public ApiEnvelope<CommentTreeResponse> createRootComment(
        @PathVariable("articleId") Long articleId,
        @RequestBody @Valid CommentCreateRequest request
    ) {
        return ApiEnvelope.ok(commentService.createRoot(articleId, request));
    }

    @PostMapping("/articles/{articleId}/comments/{parentId}")
    public ApiEnvelope<CommentTreeResponse> createReply(
        @PathVariable("articleId") Long articleId,
        @PathVariable("parentId") Long parentId,
        @RequestBody @Valid CommentCreateRequest request
    ) {
        return ApiEnvelope.ok(commentService.createReply(articleId, parentId, request));
    }

    @PutMapping("/comments/{id}")
    public ApiEnvelope<CommentTreeResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid CommentUpdateRequest request
    ) {
        return ApiEnvelope.ok(commentService.update(id, request));
    }

    @PostMapping("/comments/{id}/reactions")
    public ApiEnvelope<CommentReactionSummaryResponse> toggleReaction(
        @PathVariable("id") Long id,
        @RequestBody @Valid CommentReactionToggleRequest request
    ) {
        return ApiEnvelope.ok(commentService.toggleReaction(id, request));
    }

    @DeleteMapping("/comments/{id}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        commentService.delete(id);
        return ApiEnvelope.ok();
    }
}
