package com.mocktalkback.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.mocktalkback.global.common.type.SortOrder;

@ExtendWith(MockitoExtension.class)
class SearchNativeQueryExecutorTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private SearchSqlLoader searchSqlLoader;

    @InjectMocks
    private SearchNativeQueryExecutor searchNativeQueryExecutor;

    // 게시글 FTS 조회 시 접근 컨텍스트와 boardSlug 파라미터를 올바르게 바인딩하는지 테스트한다.
    @Test
    void fetchArticleIdsByFts_bindsAccessContextAndBoardSlug() {
        // given
        String sql = "select a.article_id from tb_articles a";
        when(searchSqlLoader.getSql(SearchSqlId.ARTICLE_IDS_FTS)).thenReturn(sql);
        when(jdbcTemplate.query(
            eq(sql),
            any(MapSqlParameterSource.class),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any()
        ))
            .thenReturn(List.of(10L, 11L));

        // when
        List<Long> result = searchNativeQueryExecutor.fetchArticleIdsByFts(
            "공지",
            0L,
            11,
            SortOrder.LATEST,
            "notice",
            7L,
            false
        );

        // then
        assertThat(result).containsExactly(10L, 11L);

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(
            eq(sql),
            paramsCaptor.capture(),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any()
        );
        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("keyword")).isEqualTo("공지");
        assertThat(params.getValue("offset")).isEqualTo(0L);
        assertThat(params.getValue("limit")).isEqualTo(11);
        assertThat(params.getValue("isOldest")).isEqualTo(false);
        assertThat(params.getValue("isAnonymous")).isEqualTo(false);
        assertThat(params.getValue("isManagerOrAdmin")).isEqualTo(false);
        assertThat(params.getValue("userId")).isEqualTo(7L);
        assertThat(params.getValue("hasBoardSlug")).isEqualTo(true);
        assertThat(params.getValue("boardSlug")).isEqualTo("notice");
    }

    // excludeIds가 비어 있을 때 센티널 값과 비활성 플래그를 바인딩하는지 테스트한다.
    @Test
    void fetchUserIdsByIlike_withEmptyExcludeIds_usesSentinelExcludeIds() {
        // given
        String sql = "select u.user_id from tb_users u";
        when(searchSqlLoader.getSql(SearchSqlId.USER_IDS_ILIKE)).thenReturn(sql);
        when(jdbcTemplate.query(
            eq(sql),
            any(MapSqlParameterSource.class),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any()
        ))
            .thenReturn(List.of(1L));

        // when
        List<Long> result = searchNativeQueryExecutor.fetchUserIdsByIlike(
            "mock",
            5L,
            6,
            SortOrder.OLDEST,
            List.of()
        );

        // then
        assertThat(result).containsExactly(1L);

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(
            eq(sql),
            paramsCaptor.capture(),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any()
        );
        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("isOldest")).isEqualTo(true);
        assertThat(params.getValue("pattern")).isEqualTo("%mock%");
        assertThat(params.getValue("hasExcludeIds")).isEqualTo(false);
        assertThat(params.getValue("excludeIds")).isEqualTo(List.of(-1L));
    }
}
