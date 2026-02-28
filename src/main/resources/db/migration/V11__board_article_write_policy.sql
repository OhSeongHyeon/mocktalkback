-- 게시판별 게시글 작성 권한 정책 추가
ALTER TABLE tb_boards
  ADD COLUMN article_write_policy VARCHAR(32) NOT NULL DEFAULT 'ALL_AUTHENTICATED';

COMMENT ON COLUMN tb_boards.article_write_policy IS '게시글 작성 권한 정책(ALL_AUTHENTICATED/MEMBER/MODERATOR/OWNER)';

-- 공지사항 게시판은 운영진 이상만 작성 가능하도록 보정
UPDATE tb_boards
SET article_write_policy = 'MODERATOR'
WHERE slug = 'notice';
