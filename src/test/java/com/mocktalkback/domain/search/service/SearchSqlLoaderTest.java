package com.mocktalkback.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchSqlLoaderTest {

    private final SearchSqlLoader searchSqlLoader = new SearchSqlLoader();

    // SQL 리소스 파일을 정상 로드하는지 테스트한다.
    @Test
    void getSql_returnsSqlFromResource() {
        // given
        SearchSqlId sqlId = SearchSqlId.BOARD_IDS_FTS;

        // when
        String sql = searchSqlLoader.getSql(sqlId);

        // then
        assertThat(sql).contains("from tb_boards b");
        assertThat(sql).contains("plainto_tsquery('simple', :keyword)");
    }

    // 같은 SQL을 여러 번 요청할 때 캐시된 문자열 인스턴스를 반환하는지 테스트한다.
    @Test
    void getSql_returnsCachedInstance() {
        // given
        SearchSqlId sqlId = SearchSqlId.USER_IDS_ILIKE;

        // when
        String first = searchSqlLoader.getSql(sqlId);
        String second = searchSqlLoader.getSql(sqlId);

        // then
        assertThat(second).isSameAs(first);
    }
}
