-- 게시글 작성자 검색
-- 목표:
-- 1) 마이그레이션 실행 시 대량 UPDATE/테이블 리라이트/대형 인덱스 생성을 피한다.
-- 2) 신규/변경 데이터는 즉시 author_search_text가 유지되도록 트리거를 건다.
-- 3) 기존 데이터는 배치 함수(fn_backfill_article_author_search_text)로 천천히 백필한다.

-- 1) 게시글 테이블에 작성자 검색 보조 컬럼 추가.
--    NULL 허용으로 두어 즉시 전체 데이터 업데이트를 강제하지 않는다.
ALTER TABLE tb_articles
  ADD COLUMN author_search_text text;

COMMENT ON COLUMN tb_articles.author_search_text IS '작성자 검색어(핸들/닉네임/이름)';

-- 2) 작성자 검색 문자열 계산 함수.
--    user_id로 사용자 정보를 읽어 "handle + display_name + user_name" 문자열을 만든다.
CREATE OR REPLACE FUNCTION fn_resolve_article_author_search_text(p_user_id bigint)
RETURNS text
LANGUAGE sql
STABLE
SET search_path = public, pg_catalog
AS $$
    SELECT concat_ws(' ', coalesce(u.handle, ''), coalesce(u.display_name, ''), coalesce(u.user_name, ''))
    FROM tb_users u
    WHERE u.user_id = p_user_id;
$$;

-- 3) 게시글 INSERT/작성자 변경 시 author_search_text 동기화 함수.
CREATE OR REPLACE FUNCTION fn_set_article_author_search_text()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_catalog
AS $$
BEGIN
    NEW.author_search_text := coalesce(fn_resolve_article_author_search_text(NEW.user_id), '');
    RETURN NEW;
END;
$$;

-- 4) 게시글 트리거 등록.
DROP TRIGGER IF EXISTS trg_tb_articles_set_author_search_text ON tb_articles;
CREATE TRIGGER trg_tb_articles_set_author_search_text
BEFORE INSERT OR UPDATE OF user_id ON tb_articles
FOR EACH ROW
EXECUTE FUNCTION fn_set_article_author_search_text();

-- 5) 사용자명 변경 시 해당 사용자의 게시글 author_search_text를 동기화하는 함수.
--    값이 실제로 바뀌지 않은 UPDATE는 즉시 종료해 불필요한 쓰기를 피한다.
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
    WHERE user_id = NEW.user_id
      AND author_search_text IS DISTINCT FROM resolved_author_search_text;

    RETURN NEW;
END;
$$;

-- 6) 사용자명 변경 트리거 등록.
DROP TRIGGER IF EXISTS trg_tb_users_sync_article_author_search_text ON tb_users;
CREATE TRIGGER trg_tb_users_sync_article_author_search_text
AFTER UPDATE OF handle, display_name, user_name ON tb_users
FOR EACH ROW
EXECUTE FUNCTION fn_sync_article_author_search_text_from_user();

-- 7) 기존 게시글 백필용 배치 함수.
--    운영 중에는 아래 함수를 여러 번 호출해 점진적으로 백필한다.
--    예) SELECT fn_backfill_article_author_search_text(2000);
CREATE OR REPLACE FUNCTION fn_backfill_article_author_search_text(batch_size integer DEFAULT 1000)
RETURNS integer
LANGUAGE plpgsql
SET search_path = public, pg_catalog
AS $$
DECLARE
    resolved_batch_size integer := greatest(coalesce(batch_size, 1000), 1);
    updated_count integer := 0;
BEGIN
    WITH target_rows AS (
        SELECT
            a.article_id,
            coalesce(fn_resolve_article_author_search_text(a.user_id), '') AS next_author_search_text
        FROM tb_articles a
        WHERE a.author_search_text IS NULL
        ORDER BY a.article_id
        LIMIT resolved_batch_size
    )
    UPDATE tb_articles a
    SET author_search_text = t.next_author_search_text
    FROM target_rows t
    WHERE a.article_id = t.article_id;

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$;

-- 8) article FTS helper 함수는 현재 search_vector(제목/본문) 기준을 유지한다.
--    작성자 검색은 애플리케이션 쿼리의 fallback(ILIKE/similarity)에서 반영한다.
ALTER FUNCTION public.fts_match_article(bigint, text)
  SET search_path = public, pg_catalog;

ALTER FUNCTION public.fts_rank_article(bigint, text)
  SET search_path = public, pg_catalog;


-- V9 적용 후!!! (중요!!!)
-- 이유: 기존의 글들 대량 UPDATE(백필)시 디비 뻗을 위험있음.
-- 안 할 때의 문제: 기존 게시글의 유저 검색은 검색누락 발생됨.

-- 1) 수동으로 하기 (손으로 일일히 해야됨)
-- SELECT fn_backfill_article_author_search_text(2000);  를 반환값이 0 이 될 때까지 직접 수행.

-- 2) 자동으로 하기 (배치 작업용 프로시저)
-- CREATE OR REPLACE PROCEDURE proc_backfill_article_author_search_text(
--     p_batch_size integer DEFAULT 2000,
--     p_sleep_ms integer DEFAULT 100
-- )
-- LANGUAGE plpgsql
-- AS $$
-- DECLARE
--     v_updated integer;
-- BEGIN
--     LOOP
--         v_updated := fn_backfill_article_author_search_text(p_batch_size);
--         RAISE NOTICE 'updated rows: %', v_updated;

--         EXIT WHEN v_updated = 0;

--         COMMIT; -- 배치 단위 커밋
--         PERFORM pg_sleep(p_sleep_ms / 1000.0); -- 부하 완화
--     END LOOP;
-- END;
-- $$;

-- 실행:
-- CALL proc_backfill_article_author_search_text(2000, 100);

-- 완료 확인:
-- SELECT COUNT(*) FROM tb_articles WHERE author_search_text IS NULL;  백필 진행률/완료 여부 확인용 쿼리, 결과가 0이면 백필 완료
