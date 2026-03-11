package com.mocktalkback.domain.board.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.board.entity.BoardSubscribeEntity;
import com.mocktalkback.domain.board.entity.QBoardEntity;
import com.mocktalkback.domain.board.entity.QBoardMemberEntity;
import com.mocktalkback.domain.board.entity.QBoardSubscribeEntity;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.global.common.util.QuerydslSortUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BoardSubscribeRepositoryImpl implements BoardSubscribeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<BoardSubscribeEntity> findAccessibleSubscribes(
        Long userId,
        Collection<BoardVisibility> visibleVisibilities,
        BoardVisibility privateVisibility,
        BoardRole ownerRole,
        BoardRole bannedRole,
        Pageable pageable
    ) {
        QBoardSubscribeEntity boardSubscribe = QBoardSubscribeEntity.boardSubscribeEntity;
        QBoardEntity board = QBoardEntity.boardEntity;
        QBoardMemberEntity boardMember = QBoardMemberEntity.boardMemberEntity;

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(boardSubscribe.user.id.eq(userId));
        predicate.and(board.deletedAt.isNull());
        predicate.and(boardMember.id.isNull().or(boardMember.boardRole.ne(bannedRole)));
        predicate.and(visibleOrPrivateOwnerCondition(board, boardMember, visibleVisibilities, privateVisibility, ownerRole));

        BooleanExpression boardMemberJoinCondition = boardMember.board.id.eq(board.id)
            .and(boardMember.user.id.eq(userId));

        List<BoardSubscribeEntity> content = queryFactory
            .selectFrom(boardSubscribe)
            .distinct()
            .join(boardSubscribe.board, board).fetchJoin()
            .leftJoin(boardMember).on(boardMemberJoinCondition)
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, boardSubscribe))
            .fetch();

        Long total = queryFactory
            .select(boardSubscribe.id.countDistinct())
            .from(boardSubscribe)
            .join(boardSubscribe.board, board)
            .leftJoin(boardMember).on(boardMemberJoinCondition)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private BooleanExpression visibleOrPrivateOwnerCondition(
        QBoardEntity board,
        QBoardMemberEntity boardMember,
        Collection<BoardVisibility> visibleVisibilities,
        BoardVisibility privateVisibility,
        BoardRole ownerRole
    ) {
        BooleanExpression privateOwner = board.visibility.eq(privateVisibility)
            .and(boardMember.boardRole.eq(ownerRole));

        if (visibleVisibilities == null || visibleVisibilities.isEmpty()) {
            return privateOwner;
        }
        return board.visibility.in(visibleVisibilities).or(privateOwner);
    }
}
