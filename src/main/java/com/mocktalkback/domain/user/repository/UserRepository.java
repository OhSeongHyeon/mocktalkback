package com.mocktalkback.domain.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long>, UserRepositoryCustom {
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByLoginId(String loginId);
    Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select user
        from UserEntity user
        join fetch user.role role
        where user.id = :userId
          and user.deletedAt is null
        """)
    Optional<UserEntity> findByIdWithRoleAndDeletedAtIsNull(@Param("userId") Long userId);

    Page<UserEntity> findByHandleContainingIgnoreCaseAndDeletedAtIsNull(
        String handle,
        Pageable pageable
    );
}
