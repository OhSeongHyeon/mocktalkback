package com.mocktalkback.domain.article.controller;

import java.time.Duration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleDetailResponse;
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
    private static final String VIEW_COOKIE_PREFIX = "article_viewed_";
    private static final Duration VIEW_COOKIE_TTL = Duration.ofHours(24);

    @PostMapping
    public ApiEnvelope<ArticleResponse> create(@RequestBody @Valid ArticleCreateRequest request) {
        return ApiEnvelope.ok(articleService.create(request));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<ArticleDetailResponse> findById(
        @PathVariable("id") Long id,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String cookieName = VIEW_COOKIE_PREFIX + id;
        boolean shouldIncrease = shouldIncreaseHit(request, cookieName);
        ArticleDetailResponse detail = articleService.findDetailById(id, shouldIncrease);
        if (shouldIncrease) {
            ResponseCookie cookie = ResponseCookie.from(cookieName, "1")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(VIEW_COOKIE_TTL)
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return ApiEnvelope.ok(detail);
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

    private boolean shouldIncreaseHit(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return true;
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return false;
            }
        }
        return true;
    }
}
