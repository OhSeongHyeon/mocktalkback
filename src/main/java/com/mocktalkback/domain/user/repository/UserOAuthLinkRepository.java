package com.mocktalkback.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.domain.user.entity.UserOAuthLinkEntity;

public interface UserOAuthLinkRepository extends JpaRepository<UserOAuthLinkEntity, Long> {
    Optional<UserOAuthLinkEntity> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByUserAndProvider(UserEntity user, String provider);
}
