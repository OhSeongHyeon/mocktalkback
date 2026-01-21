package com.mocktalkback.domain.moderation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.moderation.dto.BoardAdminArticleItemResponse;
import com.mocktalkback.domain.moderation.dto.BoardAdminCommentItemResponse;
import com.mocktalkback.domain.moderation.dto.BoardAdminNoticeUpdateRequest;
import com.mocktalkback.domain.moderation.service.BoardContentAdminService;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "BoardAdminContents", description = "커뮤니티 관리자 콘텐츠 관리 API")
public class BoardContentAdminController {

    private final BoardContentAdminService boardContentAdminService;

    @GetMapping("/boards/{boardId:\\d+}/admin/contents/articles")
    @Operation(summary = "게시글 목록", description = "게시판 게시글 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<BoardAdminArticleItemResponse>> getArticles(
        @PathVariable("boardId") Long boardId,
        @RequestParam(name = "reported", required = false) Boolean reported,
        @RequestParam(name = "notice", required = false) Boolean notice,
        @RequestParam(name = "authorId", required = false) Long authorId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(boardContentAdminService.findArticles(boardId, reported, notice, authorId, page, size));
    }

    @PutMapping("/boards/{boardId:\\d+}/admin/contents/articles/{articleId:\\d+}/notice")
    @Operation(summary = "공지 설정", description = "게시글 공지 상태를 변경합니다.")
    public ApiEnvelope<BoardAdminArticleItemResponse> updateNotice(
        @PathVariable("boardId") Long boardId,
        @PathVariable("articleId") Long articleId,
        @RequestBody @Valid BoardAdminNoticeUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardContentAdminService.updateNotice(boardId, articleId, request.notice()));
    }

    @DeleteMapping("/boards/{boardId:\\d+}/admin/contents/articles/{articleId:\\d+}")
    @Operation(summary = "게시글 삭제", description = "게시글을 소프트 삭제합니다.")
    public ApiEnvelope<Void> deleteArticle(
        @PathVariable("boardId") Long boardId,
        @PathVariable("articleId") Long articleId
    ) {
        boardContentAdminService.deleteArticle(boardId, articleId);
        return ApiEnvelope.ok();
    }

    @GetMapping("/boards/{boardId:\\d+}/admin/contents/comments")
    @Operation(summary = "댓글 목록", description = "게시판 댓글 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<BoardAdminCommentItemResponse>> getComments(
        @PathVariable("boardId") Long boardId,
        @RequestParam(name = "reported", required = false) Boolean reported,
        @RequestParam(name = "authorId", required = false) Long authorId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(boardContentAdminService.findComments(boardId, reported, authorId, page, size));
    }

    @DeleteMapping("/boards/{boardId:\\d+}/admin/contents/comments/{commentId:\\d+}")
    @Operation(summary = "댓글 삭제", description = "댓글을 소프트 삭제합니다.")
    public ApiEnvelope<Void> deleteComment(
        @PathVariable("boardId") Long boardId,
        @PathVariable("commentId") Long commentId
    ) {
        boardContentAdminService.deleteComment(boardId, commentId);
        return ApiEnvelope.ok();
    }
}
