package com.mocktalkback.domain.article.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mocktalkback.domain.article.entity.ArticleEntity;
import com.mocktalkback.domain.article.entity.QArticleEntity;
import com.mocktalkback.domain.moderation.entity.QReportEntity;
import com.mocktalkback.domain.moderation.type.ReportTargetType;
import com.mocktalkback.domain.user.entity.QUserEntity;
import com.mocktalkback.global.common.util.QuerydslSortUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ArticleEntity> findAdminBoardArticles(
        Long boardId,
        Long authorId,
        Boolean notice,
        Boolean reported,
        ReportTargetType targetType,
        Pageable pageable
    ) {
        QArticleEntity article = QArticleEntity.articleEntity;
        QUserEntity user = QUserEntity.userEntity;

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(article.board.id.eq(boardId));
        predicate.and(article.deletedAt.isNull());
        if (authorId != null) {
            predicate.and(user.id.eq(authorId));
        }
        if (notice != null) {
            predicate.and(article.notice.eq(notice));
        }
        predicate.and(reportedCondition(boardId, targetType, reported, article));

        List<ArticleEntity> content = queryFactory
            .selectFrom(article)
            .join(article.user, user).fetchJoin()
            .where(predicate)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(QuerydslSortUtils.toOrderSpecifiers(pageable, article))
            .fetch();

        Long total = queryFactory
            .select(article.count())
            .from(article)
            .join(article.user, user)
            .where(predicate)
            .fetchOne();

        long resolvedTotal = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    private BooleanBuilder reportedCondition(
        Long boardId,
        ReportTargetType targetType,
        Boolean reported,
        QArticleEntity article
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
                report.targetId.eq(article.id)
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
