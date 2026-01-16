package com.mocktalkback.domain.board.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.article.dto.BoardArticleListResponse;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardDetailResponse;
import com.mocktalkback.domain.board.dto.BoardMemberStatusResponse;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeStatusResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.service.BoardService;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final ArticleService articleService;

    @PostMapping
    public ApiEnvelope<BoardResponse> create(@RequestBody @Valid BoardCreateRequest request) {
        return ApiEnvelope.ok(boardService.create(request));
    }

    @GetMapping("/{id:\\d+}")
    public ApiEnvelope<BoardDetailResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.findById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiEnvelope<BoardDetailResponse> findBySlug(@PathVariable("slug") String slug) {
        return ApiEnvelope.ok(boardService.findBySlug(slug));
    }

    @GetMapping
    public ApiEnvelope<PageResponse<BoardResponse>> findAll(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(boardService.findAll(page, size));
    }

    @GetMapping("/{id:\\d+}/articles")
    public ApiEnvelope<BoardArticleListResponse> findArticles(
        @PathVariable("id") Long id,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(articleService.getBoardArticles(id, page, size));
    }

    @PutMapping("/{id:\\d+}")
    public ApiEnvelope<BoardResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid BoardUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardService.update(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        boardService.delete(id);
        return ApiEnvelope.ok();
    }

    @PostMapping(value = "/{id:\\d+}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiEnvelope<BoardResponse> uploadImage(
        @PathVariable("id") Long id,
        @RequestPart("boardImage") MultipartFile boardImage
    ) {
        return ApiEnvelope.ok(boardService.uploadBoardImage(id, boardImage));
    }

    @PostMapping("/{id:\\d+}/subscribe")
    public ApiEnvelope<BoardSubscribeStatusResponse> subscribe(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.subscribe(id));
    }

    @DeleteMapping("/{id:\\d+}/subscribe")
    public ApiEnvelope<BoardSubscribeStatusResponse> unsubscribe(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.unsubscribe(id));
    }

    @PostMapping("/{id:\\d+}/members")
    public ApiEnvelope<BoardMemberStatusResponse> requestJoin(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.requestJoin(id));
    }

    @PostMapping("/{id:\\d+}/members/{userId:\\d+}/approve")
    public ApiEnvelope<BoardMemberStatusResponse> approveJoin(
        @PathVariable("id") Long id,
        @PathVariable("userId") Long userId
    ) {
        return ApiEnvelope.ok(boardService.approveJoin(id, userId));
    }

    @DeleteMapping("/{id:\\d+}/members/me")
    public ApiEnvelope<Void> cancelJoin(@PathVariable("id") Long id) {
        boardService.cancelOwnMember(id);
        return ApiEnvelope.ok();
    }

    @DeleteMapping("/{id:\\d+}/members/{userId:\\d+}")
    public ApiEnvelope<Void> removeMember(
        @PathVariable("id") Long id,
        @PathVariable("userId") Long userId
    ) {
        boardService.cancelOrRejectMember(id, userId);
        return ApiEnvelope.ok();
    }
}
