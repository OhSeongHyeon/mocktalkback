package com.mocktalkback.domain.moderation.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.moderation.entity.AdminAuditLogEntity;
import com.mocktalkback.domain.moderation.type.AdminActionType;
import com.mocktalkback.domain.moderation.type.AdminTargetType;

public interface AdminAuditLogRepositoryCustom {
    Page<AdminAuditLogEntity> search(
        AdminActionType actionType,
        Long actorUserId,
        AdminTargetType targetType,
        Long targetId,
        Instant fromAt,
        Instant toAt,
        Pageable pageable
    );
}
