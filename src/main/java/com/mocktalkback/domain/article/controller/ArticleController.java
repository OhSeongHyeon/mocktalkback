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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.article.dto.ArticleCreateRequest;
import com.mocktalkback.domain.article.dto.ArticleDetailResponse;
import com.mocktalkback.domain.article.dto.ArticleBookmarkStatusResponse;
import com.mocktalkback.domain.article.dto.ArticleBookmarkItemResponse;
import com.mocktalkback.domain.article.dto.ArticleReactionSummaryResponse;
import com.mocktalkback.domain.article.dto.ArticleReactionToggleRequest;
import com.mocktalkback.domain.article.dto.ArticleResponse;
import com.mocktalkback.domain.article.dto.ArticleUpdateRequest;
import com.mocktalkback.domain.article.dto.ArticleBookmarkDeleteRequest;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.domain.article.service.ArticleBookmarkService;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Article", description = "게시글 API")
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleBookmarkService articleBookmarkService;
    private static final String VIEW_COOKIE_PREFIX = "article_viewed_";
    private static final Duration VIEW_COOKIE_TTL = Duration.ofHours(24);

    @PostMapping("/articles")
    @Operation(summary = "게시글 작성", description = "게시글을 작성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "작성 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<ArticleResponse> create(@RequestBody @Valid ArticleCreateRequest request) {
        return ApiEnvelope.ok(articleService.create(request));
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
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

    @GetMapping("/articles")
    @Operation(summary = "게시글 목록 조회", description = "게시글 전체 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class)))
    })
    public ApiEnvelope<List<ArticleResponse>> findAll() {
        return ApiEnvelope.ok(articleService.findAll());
    }

    @GetMapping("/articles/bookmarks")
    @Operation(summary = "북마크 목록 조회", description = "로그인 사용자의 북마크 목록을 페이징으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<PageResponse<ArticleBookmarkItemResponse>> findBookmarks(
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기(최대 50)", example = "10")
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(articleBookmarkService.findMyBookmarks(page, size));
    }

    @PostMapping("/articles/bookmarks/delete")
    @Operation(summary = "북마크 선택 삭제", description = "선택한 북마크를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<Void> deleteBookmarks(@RequestBody @Valid ArticleBookmarkDeleteRequest request) {
        articleBookmarkService.deleteByArticleIds(request);
        return ApiEnvelope.ok();
    }

    @DeleteMapping("/articles/bookmarks")
    @Operation(summary = "북마크 전체 삭제", description = "로그인 사용자의 모든 북마크를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<Void> deleteAllBookmarks() {
        articleBookmarkService.deleteAllByUser();
        return ApiEnvelope.ok();
    }

    @PutMapping("/articles/{id}")
    @Operation(summary = "게시글 수정", description = "게시글 내용을 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<ArticleResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid ArticleUpdateRequest request
    ) {
        return ApiEnvelope.ok(articleService.update(id, request));
    }

    @PostMapping("/articles/{id}/reactions")
    @Operation(summary = "게시글 반응 토글", description = "게시글 좋아요/싫어요 반응을 토글합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<ArticleReactionSummaryResponse> toggleReaction(
        @PathVariable("id") Long id,
        @RequestBody @Valid ArticleReactionToggleRequest request
    ) {
        return ApiEnvelope.ok(articleService.toggleReaction(id, request));
    }

    @PostMapping("/articles/{id}/bookmark")
    @Operation(summary = "게시글 북마크", description = "게시글을 북마크합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "북마크 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<ArticleBookmarkStatusResponse> bookmark(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(articleService.bookmark(id));
    }

    @DeleteMapping("/articles/{id}/bookmark")
    @Operation(summary = "게시글 북마크 해제", description = "게시글 북마크를 해제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<ArticleBookmarkStatusResponse> unbookmark(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(articleService.unbookmark(id));
    }

    @DeleteMapping("/articles/{id}")
    @Operation(summary = "게시글 삭제", description = "게시글을 소프트 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
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
