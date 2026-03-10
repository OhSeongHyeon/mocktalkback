-- V12: 중단된 CONCURRENTLY 인덱스 빌드가 남긴 invalid 인덱스를 복구한다.
-- 배경:
-- - V10 도중 프로세스가 중단되면 같은 이름의 invalid 인덱스가 남을 수 있다.
-- - 이 상태에서 V10 재실행 시 IF NOT EXISTS 때문에 invalid 인덱스를 그대로 두고 넘어갈 수 있다.

-- invalid 인덱스가 남아 있으면 먼저 제거한다.
CREATE OR REPLACE FUNCTION fn_drop_invalid_article_author_search_index()
RETURNS void
LANGUAGE plpgsql
SET search_path = public, pg_catalog
AS $$
DECLARE
    index_is_valid boolean;
BEGIN
    SELECT i.indisvalid
    INTO index_is_valid
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_index i ON i.indexrelid = c.oid
    WHERE n.nspname = 'public'
      AND c.relname = 'ix_tb_articles_author_search_text_trgm';

    IF index_is_valid = false THEN
        EXECUTE 'DROP INDEX IF EXISTS public.ix_tb_articles_author_search_text_trgm';
    END IF;
END;
$$;

SELECT fn_drop_invalid_article_author_search_index();

DROP FUNCTION fn_drop_invalid_article_author_search_index();

-- 인덱스가 없거나 invalid 제거로 사라졌다면 다시 생성한다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_tb_articles_author_search_text_trgm
ON tb_articles USING GIN (author_search_text extensions.gin_trgm_ops);

ANALYZE tb_articles;
