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
@Tag(name = "Comment", description = "댓글 API")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/articles/{articleId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 트리 구조로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ApiEnvelope<CommentPageResponse<CommentTreeResponse>> getArticleComments(
        @PathVariable("articleId") Long articleId,
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기(최대 50)", example = "10")
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(commentService.getArticleComments(articleId, page, size));
    }

    @PostMapping("/articles/{articleId}/comments")
    @Operation(summary = "댓글 작성", description = "게시글에 루트 댓글을 작성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "작성 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<CommentTreeResponse> createRootComment(
        @PathVariable("articleId") Long articleId,
        @RequestBody @Valid CommentCreateRequest request
    ) {
        return ApiEnvelope.ok(commentService.createRoot(articleId, request));
    }

    @PostMapping("/articles/{articleId}/comments/{parentId}")
    @Operation(summary = "답글 작성", description = "댓글에 답글을 작성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "작성 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<CommentTreeResponse> createReply(
        @PathVariable("articleId") Long articleId,
        @PathVariable("parentId") Long parentId,
        @RequestBody @Valid CommentCreateRequest request
    ) {
        return ApiEnvelope.ok(commentService.createReply(articleId, parentId, request));
    }

    @PutMapping("/comments/{id}")
    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<CommentTreeResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid CommentUpdateRequest request
    ) {
        return ApiEnvelope.ok(commentService.update(id, request));
    }

    @PostMapping("/comments/{id}/reactions")
    @Operation(summary = "댓글 반응 토글", description = "댓글 좋아요/싫어요 반응을 토글합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<CommentReactionSummaryResponse> toggleReaction(
        @PathVariable("id") Long id,
        @RequestBody @Valid CommentReactionToggleRequest request
    ) {
        return ApiEnvelope.ok(commentService.toggleReaction(id, request));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "댓글 삭제", description = "댓글을 소프트 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        commentService.delete(id);
        return ApiEnvelope.ok();
    }
}
