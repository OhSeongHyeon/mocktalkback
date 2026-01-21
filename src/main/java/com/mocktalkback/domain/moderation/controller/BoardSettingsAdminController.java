package com.mocktalkback.domain.moderation.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.moderation.dto.BoardAdminSettingsUpdateRequest;
import com.mocktalkback.domain.moderation.service.BoardSettingsAdminService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "BoardAdminSettings", description = "커뮤니티 관리자 게시판 설정 API")
public class BoardSettingsAdminController {

    private final BoardSettingsAdminService boardSettingsAdminService;

    @GetMapping("/boards/{boardId:\\d+}/admin/settings")
    @Operation(summary = "게시판 설정 조회", description = "게시판 설정 정보를 조회합니다.")
    public ApiEnvelope<BoardResponse> getSettings(@PathVariable("boardId") Long boardId) {
        return ApiEnvelope.ok(boardSettingsAdminService.getSettings(boardId));
    }

    @PutMapping("/boards/{boardId:\\d+}/admin/settings")
    @Operation(summary = "게시판 설정 수정", description = "게시판 이름/설명/공개 범위를 수정합니다.")
    public ApiEnvelope<BoardResponse> updateSettings(
        @PathVariable("boardId") Long boardId,
        @RequestBody @Valid BoardAdminSettingsUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardSettingsAdminService.updateSettings(boardId, request));
    }

    @PostMapping(value = "/boards/{boardId:\\d+}/admin/settings/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시판 대표 이미지 업로드", description = "게시판 대표 이미지를 업로드합니다.")
    public ApiEnvelope<BoardResponse> uploadImage(
        @PathVariable("boardId") Long boardId,
        @RequestPart("boardImage") MultipartFile boardImage
    ) {
        return ApiEnvelope.ok(boardSettingsAdminService.uploadBoardImage(boardId, boardImage));
    }

    @DeleteMapping("/boards/{boardId:\\d+}/admin/settings/image")
    @Operation(summary = "게시판 대표 이미지 삭제", description = "게시판 대표 이미지를 삭제합니다.")
    public ApiEnvelope<BoardResponse> deleteImage(@PathVariable("boardId") Long boardId) {
        return ApiEnvelope.ok(boardSettingsAdminService.deleteBoardImage(boardId));
    }
}
