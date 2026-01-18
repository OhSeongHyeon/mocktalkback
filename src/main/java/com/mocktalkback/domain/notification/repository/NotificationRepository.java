package com.mocktalkback.domain.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.notification.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @EntityGraph(attributePaths = {"sender"})
    Page<NotificationEntity> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    Page<NotificationEntity> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update NotificationEntity n
        set n.read = true
        where n.user.id = :userId and n.read = false
        """)
    int markAllRead(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from NotificationEntity n
        where n.user.id = :userId
        """)
    int deleteAllByUserId(@Param("userId") Long userId);
}
