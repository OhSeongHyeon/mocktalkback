package com.mocktalkback.domain.moderation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.article.dto.ArticleCategoryResponse;
import com.mocktalkback.domain.moderation.dto.BoardCategoryCreateRequest;
import com.mocktalkback.domain.moderation.dto.BoardCategoryUpdateRequest;
import com.mocktalkback.domain.moderation.service.BoardCategoryAdminService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "BoardAdminCategories", description = "커뮤니티 관리자 카테고리 관리 API")
public class BoardCategoryAdminController {

    private final BoardCategoryAdminService boardCategoryAdminService;

    @GetMapping("/boards/{boardId:\\d+}/admin/categories")
    @Operation(summary = "카테고리 목록", description = "게시판 카테고리 목록을 조회합니다.")
    public ApiEnvelope<List<ArticleCategoryResponse>> getCategories(@PathVariable("boardId") Long boardId) {
        return ApiEnvelope.ok(boardCategoryAdminService.findAll(boardId));
    }

    @PostMapping("/boards/{boardId:\\d+}/admin/categories")
    @Operation(summary = "카테고리 생성", description = "게시판 카테고리를 생성합니다.")
    public ApiEnvelope<ArticleCategoryResponse> createCategory(
        @PathVariable("boardId") Long boardId,
        @RequestBody @Valid BoardCategoryCreateRequest request
    ) {
        return ApiEnvelope.ok(boardCategoryAdminService.create(boardId, request));
    }

    @PutMapping("/boards/{boardId:\\d+}/admin/categories/{categoryId:\\d+}")
    @Operation(summary = "카테고리 수정", description = "게시판 카테고리를 수정합니다.")
    public ApiEnvelope<ArticleCategoryResponse> updateCategory(
        @PathVariable("boardId") Long boardId,
        @PathVariable("categoryId") Long categoryId,
        @RequestBody @Valid BoardCategoryUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardCategoryAdminService.update(boardId, categoryId, request));
    }

    @DeleteMapping("/boards/{boardId:\\d+}/admin/categories/{categoryId:\\d+}")
    @Operation(summary = "카테고리 삭제", description = "게시판 카테고리를 삭제합니다.")
    public ApiEnvelope<Void> deleteCategory(
        @PathVariable("boardId") Long boardId,
        @PathVariable("categoryId") Long categoryId
    ) {
        boardCategoryAdminService.delete(boardId, categoryId);
        return ApiEnvelope.ok();
    }
}
