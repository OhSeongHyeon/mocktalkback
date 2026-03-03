package com.mocktalkback.domain.search.service;

public enum SearchSqlId {
    BOARD_IDS_FTS("sql/search/board_ids_fts.sql"),
    BOARD_IDS_ILIKE("sql/search/board_ids_ilike.sql"),
    ARTICLE_IDS_FTS("sql/search/article_ids_fts.sql"),
    ARTICLE_IDS_ILIKE_PRIMARY("sql/search/article_ids_ilike_primary.sql"),
    ARTICLE_IDS_ILIKE_CONTENT("sql/search/article_ids_ilike_content.sql"),
    COMMENT_IDS_FTS("sql/search/comment_ids_fts.sql"),
    COMMENT_IDS_ILIKE("sql/search/comment_ids_ilike.sql"),
    USER_IDS_FTS("sql/search/user_ids_fts.sql"),
    USER_IDS_ILIKE("sql/search/user_ids_ilike.sql");

    private final String resourcePath;

    SearchSqlId(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }
}
