ALTER TABLE tb_articles
  ADD COLUMN content_source TEXT,
  ADD COLUMN content_format VARCHAR(16);

COMMENT ON COLUMN tb_articles.content IS '게시글 조회/렌더용 HTML';
COMMENT ON COLUMN tb_articles.content_source IS '게시글 작성 원본(Markdown 또는 HTML, WYSIWYG 저장 포함)';
COMMENT ON COLUMN tb_articles.content_format IS '게시글 작성 원본 포맷(HTML/MARKDOWN, WYSIWYG 저장은 HTML)';

UPDATE tb_articles
SET content_source = content,
    content_format = 'HTML'
WHERE content_source IS NULL
   OR content_format IS NULL;

ALTER TABLE tb_articles
  ALTER COLUMN content_source SET NOT NULL,
  ALTER COLUMN content_format SET NOT NULL,
  ALTER COLUMN content_format SET DEFAULT 'MARKDOWN';

