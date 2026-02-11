ALTER TABLE tb_articles
  ADD COLUMN sync_version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_articles.sync_version IS '댓글 상태 동기화 버전(스냅샷/델타용)';
