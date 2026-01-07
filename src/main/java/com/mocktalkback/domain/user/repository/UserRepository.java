package com.mocktalkback.domain.user.repository;

import com.mocktalkback.domain.user.entity.UserEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByHandle(String handle);
    
    @EntityGraph(attributePaths = "role")
    Optional<UserEntity> findWithRoleById(Long id);
}
