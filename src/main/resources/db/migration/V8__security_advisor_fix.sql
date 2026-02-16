-- Function Search Path Mutable 함수에 search_path를 고정
ALTER FUNCTION public.fts_match_board(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_rank_board(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_match_article(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_rank_article(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_match_comment(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_rank_comment(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_match_user(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_rank_user(bigint, text)
  SET search_path = public, pg_catalog;

-- Extension in Public (pg_trgm) pg_trgm를 extensions 스키마로
CREATE SCHEMA IF NOT EXISTS extensions;
ALTER EXTENSION pg_trgm SET SCHEMA extensions;

DO $$
DECLARE
  cur_schema text;
BEGIN
  SELECT n.nspname
    INTO cur_schema
  FROM pg_extension e
  JOIN pg_namespace n ON n.oid = e.extnamespace
  WHERE e.extname = 'pg_trgm';

  IF cur_schema IS NULL THEN
    EXECUTE 'CREATE EXTENSION pg_trgm WITH SCHEMA extensions';
  ELSIF cur_schema <> 'extensions' THEN
    EXECUTE 'ALTER EXTENSION pg_trgm SET SCHEMA extensions';
  END IF;
END $$;

