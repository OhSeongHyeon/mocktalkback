package com.mocktalkback.domain.moderation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.moderation.dto.AdminUserListItemResponse;
import com.mocktalkback.domain.moderation.dto.AdminUserRoleUpdateRequest;
import com.mocktalkback.domain.moderation.service.AdminUserService;
import com.mocktalkback.domain.moderation.type.AdminUserStatus;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@Tag(name = "AdminUsers", description = "사이트 관리자 사용자 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "사용자 목록", description = "관리자용 사용자 목록을 조회합니다.")
    public ApiEnvelope<PageResponse<AdminUserListItemResponse>> getUsers(
        @RequestParam(name = "status", required = false) AdminUserStatus status,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(adminUserService.search(status, keyword, page, size));
    }

    @PutMapping("/{id:\\d+}/lock")
    @Operation(summary = "사용자 잠금", description = "사용자 계정을 잠급니다.")
    public ApiEnvelope<AdminUserListItemResponse> lockUser(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(adminUserService.lock(id));
    }

    @PutMapping("/{id:\\d+}/unlock")
    @Operation(summary = "사용자 잠금 해제", description = "사용자 계정 잠금을 해제합니다.")
    public ApiEnvelope<AdminUserListItemResponse> unlockUser(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(adminUserService.unlock(id));
    }

    @PutMapping("/{id:\\d+}/role")
    @Operation(summary = "권한 변경", description = "사용자 권한을 변경합니다.")
    public ApiEnvelope<AdminUserListItemResponse> updateRole(
        @PathVariable("id") Long id,
        @RequestBody @Valid AdminUserRoleUpdateRequest request
    ) {
        return ApiEnvelope.ok(adminUserService.changeRole(id, request.roleName()));
    }
}
