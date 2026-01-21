package com.mocktalkback.domain.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByLoginId(String loginId);
    Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

    Page<UserEntity> findByHandleContainingIgnoreCaseAndDeletedAtIsNull(
        String handle,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "role")
    @Query("""
        select u
        from UserEntity u
        where u.deletedAt is null
          and (
            :keyword is null
            or lower(u.loginId) like concat('%', cast(:keyword as string), '%')
            or lower(u.handle) like concat('%', cast(:keyword as string), '%')
            or lower(u.email) like concat('%', cast(:keyword as string), '%')
            or lower(u.displayName) like concat('%', cast(:keyword as string), '%')
          )
          and (
            :status is null
            or (:status = 'ACTIVE' and u.enabled = true and u.locked = false)
            or (:status = 'LOCKED' and u.locked = true)
            or (:status = 'DISABLED' and u.enabled = false)
          )
        """)
    Page<UserEntity> findAdminUsers(
        @Param("status") String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
