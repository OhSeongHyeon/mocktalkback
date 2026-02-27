-- 게시글 검색에 작성자명(핸들/닉네임/이름) 검색을 포함한다.

ALTER TABLE tb_articles
  ADD COLUMN author_search_text text NOT NULL DEFAULT '';

COMMENT ON COLUMN tb_articles.author_search_text IS '작성자 검색어(핸들/닉네임/이름)';

UPDATE tb_articles a
SET author_search_text = concat_ws(' ', coalesce(u.handle, ''), coalesce(u.display_name, ''), coalesce(u.user_name, ''))
FROM tb_users u
WHERE u.user_id = a.user_id;

CREATE OR REPLACE FUNCTION fn_set_article_author_search_text()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_catalog
AS $$
BEGIN
    SELECT concat_ws(' ', coalesce(u.handle, ''), coalesce(u.display_name, ''), coalesce(u.user_name, ''))
      INTO NEW.author_search_text
    FROM tb_users u
    WHERE u.user_id = NEW.user_id;

    NEW.author_search_text := coalesce(NEW.author_search_text, '');
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_tb_articles_set_author_search_text ON tb_articles;
CREATE TRIGGER trg_tb_articles_set_author_search_text
BEFORE INSERT OR UPDATE OF user_id ON tb_articles
FOR EACH ROW
EXECUTE FUNCTION fn_set_article_author_search_text();

CREATE OR REPLACE FUNCTION fn_sync_article_author_search_text_from_user()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_catalog
AS $$
DECLARE
    resolved_author_search_text text;
BEGIN
    IF NEW.handle IS NOT DISTINCT FROM OLD.handle
       AND NEW.display_name IS NOT DISTINCT FROM OLD.display_name
       AND NEW.user_name IS NOT DISTINCT FROM OLD.user_name THEN
        RETURN NEW;
    END IF;

    resolved_author_search_text := concat_ws(' ', coalesce(NEW.handle, ''), coalesce(NEW.display_name, ''), coalesce(NEW.user_name, ''));

    UPDATE tb_articles
    SET author_search_text = resolved_author_search_text
    WHERE user_id = NEW.user_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_tb_users_sync_article_author_search_text ON tb_users;
CREATE TRIGGER trg_tb_users_sync_article_author_search_text
AFTER UPDATE OF handle, display_name, user_name ON tb_users
FOR EACH ROW
EXECUTE FUNCTION fn_sync_article_author_search_text_from_user();

ALTER TABLE tb_articles
  ADD COLUMN search_vector_with_author tsvector GENERATED ALWAYS AS (
      setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
      setweight(to_tsvector('simple', coalesce(content, '')), 'B') ||
      setweight(to_tsvector('simple', coalesce(author_search_text, '')), 'C')
  ) STORED;

CREATE INDEX ix_tb_articles_search_vector_with_author
  ON tb_articles USING GIN (search_vector_with_author);

CREATE INDEX ix_tb_articles_author_search_text_trgm
  ON tb_articles USING GIN (author_search_text gin_trgm_ops);

CREATE OR REPLACE FUNCTION fts_match_article(target_id bigint, keyword text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        (SELECT search_vector_with_author FROM tb_articles WHERE article_id = target_id),
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
            (SELECT search_vector_with_author FROM tb_articles WHERE article_id = target_id),
            ''::tsvector
        ),
        plainto_tsquery('simple', keyword)
    );
$$;

ALTER FUNCTION public.fts_match_article(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_rank_article(bigint, text)
  SET search_path = public, pg_catalog;
