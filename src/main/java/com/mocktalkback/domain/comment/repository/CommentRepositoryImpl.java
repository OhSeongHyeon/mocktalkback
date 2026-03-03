package com.mocktalkback.domain.comment.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.comment.entity.CommentEntity;
import com.mocktalkback.domain.comment.entity.QCommentEntity;
import com.mocktalkback.domain.moderation.entity.QReportEntity;
import com.mocktalkback.domain.moderation.type.ReportTargetType;
import com.mocktalkback.global.common.util.QuerydslSortUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CommentEntity> findAdminBoardComments(
        Long boardId,
        Long authorId,
        Boolean reported,
        ReportTargetType targetType,
        Pageable pageable
    ) {
        QCommentEntity comment = QCommentEntity.commentEntity;

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(comment.article.board.id.eq(boardId));
        predicate.and(comment.deletedAt.isNull());
        if (authorId != null) {
            predicate.and(comment.user.id.eq(authorId));
        }
        predicate.and(reportedCondition(boardId, targetType, reported, comment));

        List<CommentEntity> content = queryFactory
            .selectFrom(comment)
            .join(comment.article).fetchJoin()
            .join(comment.user).fetchJoin()
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, comment))
            .fetch();

        Long total = queryFactory
            .select(comment.count())
            .from(comment)
            .join(comment.article)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private BooleanBuilder reportedCondition(
        Long boardId,
        ReportTargetType targetType,
        Boolean reported,
        QCommentEntity comment
    ) {
        BooleanBuilder condition = new BooleanBuilder();
        if (reported == null) {
            return condition;
        }
        QReportEntity report = QReportEntity.reportEntity;
        BooleanExpression existsReported = JPAExpressions.selectOne()
            .from(report)
            .where(
                report.board.id.eq(boardId),
                report.targetType.eq(targetType),
                report.targetId.eq(comment.id)
            )
            .exists();

        if (reported) {
            condition.and(existsReported);
        } else {
            condition.and(existsReported.not());
        }
        return condition;
    }
}
