package com.mocktalkback.domain.moderation.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.domain.moderation.dto.AdminBoardCreateRequest;
import com.mocktalkback.domain.moderation.dto.AdminBoardUpdateRequest;
import com.mocktalkback.domain.moderation.service.AdminBoardService;
import com.mocktalkback.domain.moderation.type.AdminBoardSortBy;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
@Tag(name = "AdminBoards", description = "사이트 관리자 게시판 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    @GetMapping
    @Operation(summary = "게시판 목록", description = "사이트 관리자용 게시판 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<BoardResponse>> getBoards(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "visibility", required = false) BoardVisibility visibility,
        @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
        @RequestParam(name = "sort", defaultValue = "DESC") String sort,
        @RequestParam(name = "sortBy", defaultValue = "CREATED_AT") AdminBoardSortBy sortBy,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        boolean sortAsc = "ASC".equalsIgnoreCase(sort);
        return ApiEnvelope.ok(adminBoardService.findBoards(keyword, visibility, includeDeleted, sortBy, sortAsc, page, size));
    }

    @PostMapping
    @Operation(summary = "게시판 생성", description = "게시판을 생성합니다.")
    public ApiEnvelope<BoardResponse> create(
        @RequestBody @Valid AdminBoardCreateRequest request
    ) {
        return ApiEnvelope.ok(adminBoardService.create(request));
    }

    @PutMapping("/{boardId:\\d+}")
    @Operation(summary = "게시판 수정", description = "게시판 정보를 수정합니다.")
    public ApiEnvelope<BoardResponse> update(
        @PathVariable("boardId") Long boardId,
        @RequestBody @Valid AdminBoardUpdateRequest request
    ) {
        return ApiEnvelope.ok(adminBoardService.update(boardId, request));
    }

    @DeleteMapping("/{boardId:\\d+}")
    @Operation(summary = "게시판 삭제", description = "게시판을 소프트 삭제합니다.")
    public ApiEnvelope<Void> delete(@PathVariable("boardId") Long boardId) {
        adminBoardService.delete(boardId);
        return ApiEnvelope.ok();
    }

    @PostMapping(value = "/{boardId:\\d+}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "대표 이미지 업로드", description = "게시판 대표 이미지를 업로드합니다.")
    public ApiEnvelope<BoardResponse> uploadImage(
        @PathVariable("boardId") Long boardId,
        @RequestPart("boardImage") MultipartFile boardImage
    ) {
        return ApiEnvelope.ok(adminBoardService.uploadBoardImage(boardId, boardImage));
    }

    @DeleteMapping("/{boardId:\\d+}/image")
    @Operation(summary = "대표 이미지 삭제", description = "게시판 대표 이미지를 삭제합니다.")
    public ApiEnvelope<BoardResponse> deleteImage(
        @PathVariable("boardId") Long boardId
    ) {
        return ApiEnvelope.ok(adminBoardService.deleteBoardImage(boardId));
    }
}
