package com.mocktalkback.domain.moderation.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mocktalkback.domain.moderation.entity.AdminAuditLogEntity;
import com.mocktalkback.domain.moderation.type.AdminActionType;
import com.mocktalkback.domain.moderation.type.AdminTargetType;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long> {
    Page<AdminAuditLogEntity> findAllByActionType(AdminActionType actionType, Pageable pageable);

    @Query("""
        select l
        from AdminAuditLogEntity l
        where (:actionType is null or l.actionType = :actionType)
          and (:actorUserId is null or l.actorUser.id = :actorUserId)
          and (:targetType is null or l.targetType = :targetType)
          and (:targetId is null or l.targetId = :targetId)
          and l.createdAt >= coalesce(:fromAt, l.createdAt)
          and l.createdAt <= coalesce(:toAt, l.createdAt)
        """)
    Page<AdminAuditLogEntity> search(
        @Param("actionType") AdminActionType actionType,
        @Param("actorUserId") Long actorUserId,
        @Param("targetType") AdminTargetType targetType,
        @Param("targetId") Long targetId,
        @Param("fromAt") Instant fromAt,
        @Param("toAt") Instant toAt,
        Pageable pageable
    );
}
