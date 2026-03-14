package com.mocktalkback.domain.user.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.role.entity.QRoleEntity;
import com.mocktalkback.domain.user.entity.QUserEntity;
import com.mocktalkback.domain.user.entity.UserEntity;
import com.mocktalkback.global.common.util.QuerydslSortUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserEntity> findAdminUsers(
        String status,
        String keyword,
        Pageable pageable
    ) {
        QUserEntity user = QUserEntity.userEntity;
        QRoleEntity role = QRoleEntity.roleEntity;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(user.deletedAt.isNull());
        predicate.and(keywordCondition(user, keyword));
        predicate.and(statusCondition(user, status));

        List<UserEntity> content = queryFactory
            .selectFrom(user)
            .leftJoin(user.role, role).fetchJoin()
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, user))
            .fetch();

        Long total = queryFactory
            .select(user.count())
            .from(user)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private BooleanBuilder keywordCondition(QUserEntity user, String keyword) {
        BooleanBuilder condition = new BooleanBuilder();
        if (keyword == null) {
            return condition;
        }
        condition.and(
            user.loginId.lower().contains(keyword)
                .or(user.handle.lower().contains(keyword))
                .or(user.email.lower().contains(keyword))
                .or(user.displayName.lower().contains(keyword))
        );
        return condition;
    }

    private BooleanBuilder statusCondition(QUserEntity user, String status) {
        BooleanBuilder condition = new BooleanBuilder();
        if (status == null) {
            return condition;
        }
        if ("ACTIVE".equals(status)) {
            condition.and(user.enabled.isTrue().and(user.locked.isFalse()));
            return condition;
        }
        if ("LOCKED".equals(status)) {
            condition.and(user.locked.isTrue());
            return condition;
        }
        if ("DISABLED".equals(status)) {
            condition.and(user.enabled.isFalse());
            return condition;
        }
        condition.and(user.id.isNull());
        return condition;
    }
}
