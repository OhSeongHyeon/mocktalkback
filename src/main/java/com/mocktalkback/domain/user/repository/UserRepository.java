package com.mocktalkback.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByLoginId(String loginId);
}
