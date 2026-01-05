package com.mocktalkback.domain.role.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.role.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRoleName(String roleName);
    boolean existsByRoleName(String roleName);
}
