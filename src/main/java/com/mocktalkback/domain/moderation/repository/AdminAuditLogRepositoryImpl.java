package com.mocktalkback.domain.moderation.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.board.entity.QBoardEntity;
import com.mocktalkback.domain.moderation.entity.AdminAuditLogEntity;
import com.mocktalkback.domain.moderation.entity.QAdminAuditLogEntity;
import com.mocktalkback.domain.moderation.type.AdminActionType;
import com.mocktalkback.domain.moderation.type.AdminTargetType;
import com.mocktalkback.domain.user.entity.QUserEntity;
import com.mocktalkback.global.common.util.QuerydslSortUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminAuditLogRepositoryImpl implements AdminAuditLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdminAuditLogEntity> search(
        AdminActionType actionType,
        Long actorUserId,
        AdminTargetType targetType,
        Long targetId,
        Instant fromAt,
        Instant toAt,
        Pageable pageable
    ) {
        QAdminAuditLogEntity auditLog = QAdminAuditLogEntity.adminAuditLogEntity;
        QUserEntity actorUser = QUserEntity.userEntity;
        QBoardEntity board = QBoardEntity.boardEntity;

        BooleanBuilder predicate = new BooleanBuilder();
        if (actionType != null) {
            predicate.and(auditLog.actionType.eq(actionType));
        }
        if (actorUserId != null) {
            predicate.and(auditLog.actorUser.id.eq(actorUserId));
        }
        if (targetType != null) {
            predicate.and(auditLog.targetType.eq(targetType));
        }
        if (targetId != null) {
            predicate.and(auditLog.targetId.eq(targetId));
        }
        if (fromAt != null) {
            predicate.and(auditLog.createdAt.goe(fromAt));
        }
        if (toAt != null) {
            predicate.and(auditLog.createdAt.loe(toAt));
        }

        List<AdminAuditLogEntity> content = queryFactory
            .selectFrom(auditLog)
            .join(auditLog.actorUser, actorUser).fetchJoin()
            .leftJoin(auditLog.board, board).fetchJoin()
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, auditLog))
            .fetch();

        Long total = queryFactory
            .select(auditLog.count())
            .from(auditLog)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }
}
