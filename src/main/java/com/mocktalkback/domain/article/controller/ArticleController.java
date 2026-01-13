package com.mocktalkback.domain.article.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.dto.ArticleUpdateRequest;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public ApiEnvelope<ArticleResponse> create(@RequestBody @Valid ArticleCreateRequest request) {
        return ApiEnvelope.ok(articleService.create(request));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<ArticleResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(articleService.findById(id));
    }

    @GetMapping
    public ApiEnvelope<List<ArticleResponse>> findAll() {
        return ApiEnvelope.ok(articleService.findAll());
    }

    @PutMapping("/{id}")
    public ApiEnvelope<ArticleResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid ArticleUpdateRequest request
    ) {
        return ApiEnvelope.ok(articleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        articleService.delete(id);
        return ApiEnvelope.ok();
    }
}
