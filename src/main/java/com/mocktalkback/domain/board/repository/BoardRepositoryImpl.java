package com.mocktalkback.domain.board.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.board.entity.BoardEntity;
import com.mocktalkback.domain.board.entity.QBoardEntity;
import com.mocktalkback.domain.board.entity.QBoardMemberEntity;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.board.type.BoardVisibility;
import com.mocktalkback.global.common.util.QuerydslSortUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<BoardEntity> findAdminBoards(
        String keyword,
        BoardVisibility visibility,
        boolean includeDeleted,
        Pageable pageable
    ) {
        QBoardEntity board = QBoardEntity.boardEntity;
        BooleanBuilder predicate = new BooleanBuilder();
        if (!includeDeleted) {
            predicate.and(board.deletedAt.isNull());
        }
        predicate.and(keywordCondition(board, keyword));
        if (visibility != null) {
            predicate.and(board.visibility.eq(visibility));
        }

        List<BoardEntity> content = queryFactory
            .selectFrom(board)
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, board))
            .fetch();

        Long total = queryFactory
            .select(board.count())
            .from(board)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    @Override
    public Page<BoardEntity> findAccessibleBoards(
        Long userId,
        Collection<BoardVisibility> visibleVisibilities,
        BoardVisibility privateVisibility,
        BoardRole ownerRole,
        BoardRole bannedRole,
        Pageable pageable
    ) {
        QBoardEntity board = QBoardEntity.boardEntity;
        QBoardMemberEntity boardMember = QBoardMemberEntity.boardMemberEntity;

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(board.deletedAt.isNull());
        predicate.and(boardMember.id.isNull().or(boardMember.boardRole.ne(bannedRole)));
        predicate.and(visibleOrPrivateOwnerCondition(board, boardMember, visibleVisibilities, privateVisibility, ownerRole));

        BooleanExpression boardMemberJoinCondition = boardMember.board.id.eq(board.id)
            .and(boardMember.user.id.eq(userId));

        List<BoardEntity> content = queryFactory
            .select(board)
            .distinct()
            .from(board)
            .leftJoin(boardMember).on(boardMemberJoinCondition)
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, board))
            .fetch();

        Long total = queryFactory
            .select(board.id.countDistinct())
            .from(board)
            .leftJoin(boardMember).on(boardMemberJoinCondition)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private BooleanBuilder keywordCondition(QBoardEntity board, String keyword) {
        BooleanBuilder condition = new BooleanBuilder();
        if (keyword == null) {
            return condition;
        }
        condition.and(
            board.boardName.lower().contains(keyword)
                .or(board.slug.lower().contains(keyword))
        );
        return condition;
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
