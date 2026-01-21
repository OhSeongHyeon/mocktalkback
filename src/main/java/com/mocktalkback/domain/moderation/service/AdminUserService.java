package com.mocktalkback.domain.moderation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mocktalkback.domain.moderation.dto.AdminUserListItemResponse;
import com.mocktalkback.domain.moderation.type.AdminUserStatus;
import com.mocktalkback.domain.role.entity.RoleEntity;
import com.mocktalkback.domain.role.repository.RoleRepository;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.repository.UserRepository;
import com.mocktalkback.global.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItemResponse> search(
        AdminUserStatus status,
        String keyword,
        int page,
        int size
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        Pageable pageable = PageRequest.of(
            normalizedPage,
            normalizedSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String statusCode = status != null ? status.name() : null;
        Page<UserEntity> result = userRepository.findAdminUsers(statusCode, normalizedKeyword, pageable);
        Page<AdminUserListItemResponse> mapped = result.map(AdminUserListItemResponse::from);
        return PageResponse.from(mapped);
    }

    @Transactional
    public AdminUserListItemResponse lock(Long userId) {
        UserEntity user = getUser(userId);
        user.lock();
        return AdminUserListItemResponse.from(user);
    }

    @Transactional
    public AdminUserListItemResponse unlock(Long userId) {
        UserEntity user = getUser(userId);
        user.unlock();
        return AdminUserListItemResponse.from(user);
    }

    @Transactional
    public AdminUserListItemResponse changeRole(Long userId, String roleName) {
        UserEntity user = getUser(userId);
        RoleEntity role = roleRepository.findByRoleName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("권한이 존재하지 않습니다."));
        user.changeRole(role);
        return AdminUserListItemResponse.from(user);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private int normalizePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return size;
    }
}
