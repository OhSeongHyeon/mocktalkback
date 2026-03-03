package com.mocktalkback.global.common.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.PathBuilder;

public final class QuerydslSortUtils {

    private QuerydslSortUtils() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static OrderSpecifier<?>[] toOrderSpecifiers(Pageable pageable, EntityPath<?> rootPath) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        PathBuilder<?> pathBuilder = new PathBuilder<>(rootPath.getType(), rootPath.getMetadata());
        for (Sort.Order sortOrder : pageable.getSort()) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
            ComparablePath path = pathBuilder.getComparable(sortOrder.getProperty(), Comparable.class);
            orderSpecifiers.add(new OrderSpecifier(direction, path));
        }
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
