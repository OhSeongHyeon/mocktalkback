
-- role seed
INSERT INTO tb_role (role_name, auth_bit, description)
VALUES
  ('USER', 1, '기본 사용자(읽기)'),
  ('WRITER', 3, '작성 가능(읽기+쓰기)'),
  ('MANAGER', 7, '매니저(읽기+쓰기+삭제)'),
  ('ADMIN', 15, '전체 권한');

-- Users admin
INSERT INTO tb_users (
  role_id,
  login_id,
  email,
  pw_hash,
  user_name,
  display_name,
  handle,
  user_point,
  is_email_verified,
  is_enabled,
  is_locked
)
VALUES
  ((SELECT role_id FROM tb_role WHERE role_name = 'ADMIN'),
   'admin', 'adm@dummy.du', '$argon2id$v=19$m=16384,t=2,p=1$uXob/dB7g0RnR5HNjR1daw$gasEEOjrRqtZXCkUKe/6Ndpe3hCSLyUl5QG901b4YL4',
   'Admin Admin', 'Admin', 'admin', 99999, false, true, false);

-- Boards 기초 게시판, 공지, 문의
INSERT INTO tb_boards (board_name, slug, description, visibility)
VALUES
  ('공지사항', 'notice', '운영 공지 및 업데이트 안내', 'PUBLIC'),
  ('문의 게시판', 'inquiry', '서비스 이용 문의/건의', 'PUBLIC');

-- (Optional) admin을 공지/문의 게시판 OWNER로 등록
INSERT INTO tb_board_members (user_id, board_id, granted_by_user_id, board_role)
SELECT u.user_id, b.board_id, u.user_id, 'OWNER'
FROM tb_users u
JOIN tb_boards b ON b.slug IN ('notice', 'inquiry')
WHERE u.login_id = 'admin';

-- (Optional) 공지사항 기본 글 1개
INSERT INTO tb_articles (board_id, user_id, title, content, is_notice, visibility)
VALUES (
  (SELECT board_id FROM tb_boards WHERE slug = 'notice'),
  (SELECT user_id  FROM tb_users WHERE login_id = 'admin'),
  '환영합니다',
  '공지사항 게시판입니다. 업데이트/점검/정책 변경은 여기에서 안내됩니다.',
  true,
  'PUBLIC'
);




















































