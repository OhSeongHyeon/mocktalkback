package com.mocktalkback.domain.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.user.entity.UserEntity;

public interface UserRepositoryCustom {
    Page<UserEntity> findAdminUsers(
        String status,
        String keyword,
        Pageable pageable
    );
}
