package com.mocktalkback.domain.moderation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.moderation.entity.AdminAuditLogEntity;
import com.mocktalkback.domain.moderation.type.AdminActionType;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long>, AdminAuditLogRepositoryCustom {
    Page<AdminAuditLogEntity> findAllByActionType(AdminActionType actionType, Pageable pageable);
}
