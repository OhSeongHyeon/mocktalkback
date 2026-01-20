CREATE OR REPLACE FUNCTION fts_match_board(target_id bigint, keyword text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        (SELECT search_vector FROM tb_boards WHERE board_id = target_id),
        ''::tsvector
    ) @@ plainto_tsquery('simple', keyword);
$$;

CREATE OR REPLACE FUNCTION fts_rank_board(target_id bigint, keyword text)
RETURNS double precision
LANGUAGE sql
STABLE
AS $$
    SELECT ts_rank(
        COALESCE(
            (SELECT search_vector FROM tb_boards WHERE board_id = target_id),
            ''::tsvector
        ),
        plainto_tsquery('simple', keyword)
    );
$$;

CREATE OR REPLACE FUNCTION fts_match_article(target_id bigint, keyword text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        (SELECT search_vector FROM tb_articles WHERE article_id = target_id),
        ''::tsvector
    ) @@ plainto_tsquery('simple', keyword);
$$;

CREATE OR REPLACE FUNCTION fts_rank_article(target_id bigint, keyword text)
RETURNS double precision
LANGUAGE sql
STABLE
AS $$
    SELECT ts_rank(
        COALESCE(
            (SELECT search_vector FROM tb_articles WHERE article_id = target_id),
            ''::tsvector
        ),
        plainto_tsquery('simple', keyword)
    );
$$;

CREATE OR REPLACE FUNCTION fts_match_comment(target_id bigint, keyword text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        (SELECT search_vector FROM tb_comments WHERE comment_id = target_id),
        ''::tsvector
    ) @@ plainto_tsquery('simple', keyword);
$$;

CREATE OR REPLACE FUNCTION fts_rank_comment(target_id bigint, keyword text)
RETURNS double precision
LANGUAGE sql
STABLE
AS $$
    SELECT ts_rank(
        COALESCE(
            (SELECT search_vector FROM tb_comments WHERE comment_id = target_id),
            ''::tsvector
        ),
        plainto_tsquery('simple', keyword)
    );
$$;

CREATE OR REPLACE FUNCTION fts_match_user(target_id bigint, keyword text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        (SELECT search_vector FROM tb_users WHERE user_id = target_id),
        ''::tsvector
    ) @@ plainto_tsquery('simple', keyword);
$$;

CREATE OR REPLACE FUNCTION fts_rank_user(target_id bigint, keyword text)
RETURNS double precision
LANGUAGE sql
STABLE
AS $$
    SELECT ts_rank(
        COALESCE(
            (SELECT search_vector FROM tb_users WHERE user_id = target_id),
            ''::tsvector
        ),
        plainto_tsquery('simple', keyword)
    );
$$;
