package com.mocktalkback.domain.search.service;

import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.mocktalkback.global.common.type.SortOrder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchNativeQueryExecutor {

    private static final List<Long> EMPTY_EXCLUDE_IDS = List.of(-1L);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchSqlLoader searchSqlLoader;

    public List<Long> fetchBoardIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        Long userId,
        boolean managerOrAdmin
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        addAccessContextParams(params, userId, managerOrAdmin);
        return fetchIds(SearchSqlId.BOARD_IDS_FTS, params);
    }

    public List<Long> fetchBoardIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        Long userId,
        boolean managerOrAdmin,
        List<Long> excludeIds
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        params.addValue("pattern", "%" + keyword + "%");
        addAccessContextParams(params, userId, managerOrAdmin);
        addExcludeIdsParams(params, excludeIds);
        return fetchIds(SearchSqlId.BOARD_IDS_ILIKE, params);
    }

    public List<Long> fetchArticleIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        Long userId,
        boolean managerOrAdmin
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        addAccessContextParams(params, userId, managerOrAdmin);
        addBoardSlugParams(params, boardSlug);
        return fetchIds(SearchSqlId.ARTICLE_IDS_FTS, params);
    }

    public List<Long> fetchArticleIdsByIlikePrimary(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        Long userId,
        boolean managerOrAdmin,
        List<Long> excludeIds
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        params.addValue("pattern", "%" + keyword + "%");
        addAccessContextParams(params, userId, managerOrAdmin);
        addBoardSlugParams(params, boardSlug);
        addExcludeIdsParams(params, excludeIds);
        return fetchIds(SearchSqlId.ARTICLE_IDS_ILIKE_PRIMARY, params);
    }

    public List<Long> fetchArticleIdsByIlikeContent(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        Long userId,
        boolean managerOrAdmin,
        List<Long> excludeIds
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        params.addValue("pattern", "%" + keyword + "%");
        addAccessContextParams(params, userId, managerOrAdmin);
        addBoardSlugParams(params, boardSlug);
        addExcludeIdsParams(params, excludeIds);
        return fetchIds(SearchSqlId.ARTICLE_IDS_ILIKE_CONTENT, params);
    }

    public List<Long> fetchCommentIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        Long userId,
        boolean managerOrAdmin
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        addAccessContextParams(params, userId, managerOrAdmin);
        addBoardSlugParams(params, boardSlug);
        return fetchIds(SearchSqlId.COMMENT_IDS_FTS, params);
    }

    public List<Long> fetchCommentIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        String boardSlug,
        Long userId,
        boolean managerOrAdmin,
        List<Long> excludeIds
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        params.addValue("pattern", "%" + keyword + "%");
        addAccessContextParams(params, userId, managerOrAdmin);
        addBoardSlugParams(params, boardSlug);
        addExcludeIdsParams(params, excludeIds);
        return fetchIds(SearchSqlId.COMMENT_IDS_ILIKE, params);
    }

    public List<Long> fetchUserIdsByFts(
        String keyword,
        long offset,
        int limit,
        SortOrder order
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        return fetchIds(SearchSqlId.USER_IDS_FTS, params);
    }

    public List<Long> fetchUserIdsByIlike(
        String keyword,
        long offset,
        int limit,
        SortOrder order,
        List<Long> excludeIds
    ) {
        MapSqlParameterSource params = createCommonParams(keyword, offset, limit, order);
        params.addValue("pattern", "%" + keyword + "%");
        addExcludeIdsParams(params, excludeIds);
        return fetchIds(SearchSqlId.USER_IDS_ILIKE, params);
    }

    private MapSqlParameterSource createCommonParams(String keyword, long offset, int limit, SortOrder order) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("keyword", keyword);
        params.addValue("offset", offset);
        params.addValue("limit", limit);
        params.addValue("isOldest", order == SortOrder.OLDEST);
        return params;
    }

    private void addAccessContextParams(MapSqlParameterSource params, Long userId, boolean managerOrAdmin) {
        boolean anonymous = userId == null;
        params.addValue("isAnonymous", anonymous);
        params.addValue("isManagerOrAdmin", managerOrAdmin);
        params.addValue("userId", userId, Types.BIGINT);
    }

    private void addBoardSlugParams(MapSqlParameterSource params, String boardSlug) {
        boolean hasBoardSlug = StringUtils.hasText(boardSlug);
        params.addValue("hasBoardSlug", hasBoardSlug);
        params.addValue("boardSlug", hasBoardSlug ? boardSlug : null, Types.VARCHAR);
    }

    private void addExcludeIdsParams(MapSqlParameterSource params, List<Long> excludeIds) {
        boolean hasExcludeIds = excludeIds != null && !excludeIds.isEmpty();
        params.addValue("hasExcludeIds", hasExcludeIds);
        params.addValue("excludeIds", hasExcludeIds ? excludeIds : EMPTY_EXCLUDE_IDS);
    }

    private List<Long> fetchIds(SearchSqlId sqlId, MapSqlParameterSource params) {
        String sql = searchSqlLoader.getSql(sqlId);
        return jdbcTemplate.query(sql, params, (resultSet, rowNum) -> resultSet.getLong(1));
    }
}
