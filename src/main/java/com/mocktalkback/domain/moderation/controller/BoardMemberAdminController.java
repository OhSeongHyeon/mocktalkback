package com.mocktalkback.domain.moderation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.moderation.dto.BoardMemberListItemResponse;
import com.mocktalkback.domain.moderation.dto.BoardMemberRoleUpdateRequest;
import com.mocktalkback.domain.moderation.dto.BoardMemberStatusRequest;
import com.mocktalkback.domain.moderation.service.BoardMemberAdminService;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/{boardId:\\d+}/admin/members")
@Tag(name = "BoardAdminMembers", description = "커뮤니티 관리자 멤버 관리 API")
public class BoardMemberAdminController {

    private final BoardMemberAdminService boardMemberAdminService;

    @GetMapping
    @Operation(summary = "멤버 목록", description = "게시판 멤버 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<BoardMemberListItemResponse>> getMembers(
        @PathVariable("boardId") Long boardId,
        @RequestParam(name = "status", required = false) BoardRole status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(boardMemberAdminService.findMembers(boardId, status, page, size));
    }

    @PutMapping("/{memberId:\\d+}/approve")
    @Operation(summary = "가입 승인", description = "가입 요청을 승인합니다.")
    public ApiEnvelope<BoardMemberListItemResponse> approve(
        @PathVariable("boardId") Long boardId,
        @PathVariable("memberId") Long memberId
    ) {
        return ApiEnvelope.ok(boardMemberAdminService.approve(boardId, memberId));
    }

    @PutMapping("/{memberId:\\d+}/reject")
    @Operation(summary = "가입 거절", description = "가입 요청을 거절합니다.")
    public ApiEnvelope<Void> reject(
        @PathVariable("boardId") Long boardId,
        @PathVariable("memberId") Long memberId
    ) {
        boardMemberAdminService.reject(boardId, memberId);
        return ApiEnvelope.ok();
    }

    @PutMapping("/{memberId:\\d+}/role")
    @Operation(summary = "역할 변경", description = "멤버 역할을 변경합니다.")
    public ApiEnvelope<BoardMemberListItemResponse> changeRole(
        @PathVariable("boardId") Long boardId,
        @PathVariable("memberId") Long memberId,
        @RequestBody @Valid BoardMemberRoleUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardMemberAdminService.changeRole(boardId, memberId, request.boardRole()));
    }

    @PutMapping("/{memberId:\\d+}/status")
    @Operation(summary = "상태 변경", description = "멤버 차단/해제를 처리합니다.")
    public ApiEnvelope<BoardMemberListItemResponse> changeStatus(
        @PathVariable("boardId") Long boardId,
        @PathVariable("memberId") Long memberId,
        @RequestBody @Valid BoardMemberStatusRequest request
    ) {
        return ApiEnvelope.ok(boardMemberAdminService.changeStatus(boardId, memberId, request.boardRole()));
    }
}
