-- V10: 게시글 작성자 검색 보조 인덱스 추가
-- 주의:
-- - CONCURRENTLY는 트랜잭션 밖에서 실행되어야 하므로
--   같은 이름의 .sql.conf에서 executeInTransaction=false를 지정한다.

-- author_search_text ILIKE 검색 가속용 trigram 인덱스
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_tb_articles_author_search_text_trgm
ON tb_articles USING GIN (author_search_text extensions.gin_trgm_ops);

-- 플래너 통계 최신화
ANALYZE tb_articles;
