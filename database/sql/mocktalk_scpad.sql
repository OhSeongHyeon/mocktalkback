-- ===============
-- postgres
-- ===============

-- 스키마 드랍 후 생성
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- 테이블 싹다 드랍
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT schemaname, tablename
    FROM pg_tables
    WHERE schemaname = 'public'
  LOOP
    EXECUTE format('DROP TABLE IF EXISTS %I.%I CASCADE;', r.schemaname, r.tablename);
  END LOOP;
END $$;


SELECT current_database();
SELECT current_schema();

-- 테이블 보기
SELECT tablename
FROM pg_catalog.pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- 컬럼타입
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default,
    character_maximum_length
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'tb_users'
ORDER BY ordinal_position;

-- 인덱스보기
SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'tb_users'
ORDER BY indexname;

-- fk 보기
SELECT
  tc.constraint_name,
  tc.table_name,
  kcu.column_name,
  ccu.table_name AS referenced_table_name,
  ccu.column_name AS referenced_column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name = kcu.constraint_name
 AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage ccu
  ON ccu.constraint_name = tc.constraint_name
 AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
  AND tc.table_name = 'tb_users'
ORDER BY tc.constraint_name, kcu.ordinal_position;

SELECT * FROM tb_role;

-- 테스트용
INSERT INTO tb_role (role_name, auth_bit, description, created_at, updated_at)
VALUES ('MANUAL_DB_NOW', 31, 'DB now()로 넣은 테스트', now(), now());

INSERT INTO tb_role (role_name, auth_bit, description, created_at, updated_at)
VALUES (
  'MANUAL_EXPLICIT_TIME',
  31,
  '시간 직접 지정 테스트',
  TIMESTAMPTZ '2026-01-06 05:30:03.654+09',
  TIMESTAMPTZ '2026-01-06 05:30:03.654+09'
);

SELECT * FROM tb_users;
SELECT * FROM tb_user_oauth_links;
SELECT * FROM tb_articles;
SELECT * FROM tb_boards;
SELECT * FROM tb_board_members;
SELECT * FROM tb_board_files;
SELECT * FROM tb_user_files;
SELECT * FROM tb_files;
SELECT * FROM tb_file_variants;
SELECT * FROM tb_notification;
SELECT * FROM tb_comments;
SELECT * FROM tb_board_subscribes;
SELECT * FROM tb_article_reactions;


UPDATE tb_users
SET is_enabled = TRUE,
is_locked = FALSE,
deleted_at = NULL
WHERE login_id = 'seed_writer';

DELETE FROM tb_boards WHERE board_id > 2;
DELETE FROM tb_notification WHERE 1=1;
DELETE FROM tb_articles WHERE 1=1;
DELETE FROM tb_board_files WHERE 1=1;
DELETE FROM tb_user_files WHERE 1=1;
DELETE FROM tb_files WHERE 1=1;
DELETE FROM tb_file_variants WHERE 1=1;


SELECT column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_name='tb_boards' AND column_name='search_vector';

---------------------

EXPLAIN (ANALYZE, BUFFERS)
select count(distinct be1_0.board_id)
from tb_boards be1_0
left join tb_board_members bme1_0
on bme1_0.board_id=be1_0.board_id
and bme1_0.user_id=2
where be1_0.deleted_at is null
and (
    fts_match_board(be1_0.board_id, 'board')=true
    or (
    lower(be1_0.board_name) like '%board%' escape '!'
    or lower(be1_0.slug) like '%board%' escape '!'
    or lower(be1_0.description) like '%board%' escape '!'
    )
)
and (
    bme1_0.board_manager_id is null
    or bme1_0.board_role<>'BANNED'
)
and (
    be1_0.visibility in ('PUBLIC','GROUP')
    or be1_0.visibility='PRIVATE' and bme1_0.board_role='OWNER'
);

EXPLAIN (ANALYZE, BUFFERS)
select
    be1_0.board_id, be1_0.board_name, be1_0.created_at, be1_0.deleted_at,
    be1_0.description, be1_0.slug, be1_0.updated_at, be1_0.visibility
from tb_boards be1_0
left join tb_board_members bme1_0
on bme1_0.board_id=be1_0.board_id
and bme1_0.user_id=2
where be1_0.deleted_at is null
and (
    fts_match_board(be1_0.board_id, 'board')=true
    or (
    lower(be1_0.board_name) like '%board%' escape '!'
    or lower(be1_0.slug) like '%board%' escape '!'
    or lower(be1_0.description) like '%board%' escape '!'
    )
)
and (
    bme1_0.board_manager_id is null
    or bme1_0.board_role<>'BANNED'
)
and (
    be1_0.visibility in ('PUBLIC','GROUP')
    or be1_0.visibility='PRIVATE' and bme1_0.board_role='OWNER'
)
order by
    fts_rank_board(be1_0.board_id, 'board') desc,
    be1_0.created_at desc,
    be1_0.updated_at desc,
    be1_0.board_id desc
offset 0 rows fetch first 10 rows only;

-----------------------------------------

SELECT * FROM tb_users;

--UPDATE tb_users SET user_point = floor(random() * 20001)::int;
UPDATE tb_users SET user_point = 0::int;

UPDATE tb_users SET user_point = (floor(random() * 10001)::int) * 100;

UPDATE tb_users SET user_point = 999999::int WHERE login_id = 'admin';



----------------------------------------------------

-- Seed
-- Roles
INSERT INTO tb_role (role_name, auth_bit, description)
VALUES
  ('USER', 1, '기본 사용자(읽기)'),
  ('WRITER', 3, '작성 가능(읽기+쓰기)'),
  ('MANAGER', 7, '모더레이터(읽기+쓰기+삭제)'),
  ('ADMIN', 15, '전체 권한');

-- Users 어드민
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
   'Admin Admin', 'Admin', 'admin', 0, false, true, false);

-- Boards 기초 게시판, 공지, 문의
INSERT INTO tb_board (board_name, slug, description, visibility)
VALUES
  ('공지사항', '/b/notice', '운영 공지 및 업데이트 안내', 'PUBLIC'),
  ('문의 게시판', '/b/inquiry', '서비스 이용 문의/건의', 'PUBLIC');

-- (Optional) admin을 공지/문의 게시판 OWNER로 등록
INSERT INTO tb_board_members (user_id, board_id, granted_by_user_id, board_role)
SELECT u.user_id, b.board_id, u.user_id, 'OWNER'
FROM tb_users u
JOIN tb_board b ON b.slug IN ('/b/notice', '/b/inquiry')
WHERE u.login_id = 'admin';

-- (Optional) 공지사항 기본 글 1개
INSERT INTO tb_articles (board_id, user_id, title, content, is_notice, visibility)
VALUES (
  (SELECT board_id FROM tb_board WHERE slug = '/b/notice'),
  (SELECT user_id  FROM tb_users WHERE login_id = 'admin'),
  '환영합니다',
  '공지사항 게시판입니다. 업데이트/점검/정책 변경은 여기에서 안내됩니다.',
  true,
  'PUBLIC'
);



-- Users
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
  ((SELECT role_id FROM tb_role WHERE role_name = 'USER'),
   'seed_user', 'seed_user@example.com', '$argon2id$v=19$m=16384,t=2,p=1$uXob/dB7g0RnR5HNjR1daw$gasEEOjrRqtZXCkUKe/6Ndpe3hCSLyUl5QG901b4YL4',
   'Seed User', 'SeedUser', 'seed_user', 1100, false, true, false),
  ((SELECT role_id FROM tb_role WHERE role_name = 'WRITER'),
   'seed_writer', 'seed_writer@example.com', '$argon2id$v=19$m=16384,t=2,p=1$MBgerTYHMKeCFXqevHRcPw$nrbsGsSKsslFN/W1K5/322yZlbPM7R70tWtvpeyb9dQ',
   'Seed Writer', 'SeedWriter', 'seed_writer', 1200, false, true, false),
  ((SELECT role_id FROM tb_role WHERE role_name = 'MANAGER'),
   'seed_moderator', 'seed_moderator@example.com', '$argon2id$v=19$m=16384,t=2,p=1$k0FBG6+eySQ1LuOVrjQJdg$5euufr9CBtbjHhS7x7D/mpvOR9y/yrIKP06w3jHDQh4',
   'Seed Moderator', 'SeedMod', 'seed_moderator', 1300, false, true, false),
  ((SELECT role_id FROM tb_role WHERE role_name = 'ADMIN'),
   'seed_admin', 'seed_admin@example.com', '$argon2id$v=19$m=16384,t=2,p=1$h3R1ST6MeVCTl8O0okEVRw$FIY6OdQ5Mm8PPRHS8gZInLqivnhcukBYUhBjMfcxVK8',
   'Seed Admin', 'SeedAdmin', 'seed_admin', 1400, false, true, false);

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
SELECT
  ((gs - 1) % 4) + 1,
  'login_id_' || gs,
  'email_' || gs,
  'pw_' || gs,
  'user_name_' || gs,
  'dis_name_' || gs,
  'handle_' || gs,
  100,
  false,
  true,
  false
FROM generate_series(20, 1000000) gs;

SELECT * FROM tb_users;

-- File classes
INSERT INTO tb_file_classes (file_class_code, file_class_name, description, media_kind)
SELECT
  'CLASS_' || gs,
  'File Class ' || gs,
  'Class desc ' || gs,
  CASE (gs - 1) % 6
    WHEN 0 THEN 'IMAGE'
    WHEN 1 THEN 'VIDEO'
    WHEN 2 THEN 'AUDIO'
    WHEN 3 THEN 'DOCUMENT'
    WHEN 4 THEN 'ARCHIVE'
    ELSE 'OTHER'
  END
FROM generate_series(1, 10000) gs;

-- Files
INSERT INTO tb_files (file_class_id, file_name, storage_key, file_size, mime_type)
SELECT
  gs,
  'file_' || gs || '.dat',
  'files/file_' || gs || '.dat',
  gs * 1024,
  CASE (gs - 1) % 3
    WHEN 0 THEN 'image/png'
    WHEN 1 THEN 'application/pdf'
    ELSE 'application/octet-stream'
  END
FROM generate_series(1, 10000) gs;

-- Boards
INSERT INTO tb_boards (board_name, slug, description, visibility)
SELECT
  'Board ' || gs,
  'board-' || gs,
  'Board desc ' || gs,
  CASE (gs - 1) % 4
    WHEN 0 THEN 'PUBLIC'
    WHEN 1 THEN 'GROUP'
    WHEN 2 THEN 'PRIVATE'
    ELSE 'UNLISTED'
  END
FROM generate_series(3, 10000) gs;


-- Board files
INSERT INTO tb_board_files (file_id, board_id)
SELECT gs, gs FROM generate_series(1, 100) gs;

-- Board members
INSERT INTO tb_board_members (user_id, board_id, granted_by_user_id, board_role)
SELECT
  ((gs - 1) % 3) + 1,
  gs,
  CASE WHEN gs = 1 THEN NULL ELSE 1 END,
  CASE (gs - 1) % 4
    WHEN 0 THEN 'OWNER'
    WHEN 1 THEN 'MODERATOR'
    WHEN 2 THEN 'MEMBER'
    ELSE 'BANNED'
  END
FROM generate_series(1, 10000) gs;

-- Board subscribes
INSERT INTO tb_board_subscribes (user_id, board_id)
SELECT ((gs - 1) % 3) + 1, gs FROM generate_series(1, 10000) gs;

-- Article categories
INSERT INTO tb_article_categories (board_id, category_name)
SELECT
  gs,
  'Category ' || gs
FROM generate_series(1, 10000) gs;

-- Articles
INSERT INTO tb_articles (
  board_id,
  user_id,
  article_category_id,
  visibility,
  title,
  content,
  hit,
  is_notice
)
SELECT
  ((gs - 1) % 4) + 1,
  ((gs - 1) % 3) + 1,
  NULL,
  CASE (gs - 1) % 4
    WHEN 0 THEN 'PUBLIC'
    WHEN 1 THEN 'MEMBERS'
    WHEN 2 THEN 'MANAGERS'
    ELSE 'ADMINS'
  END,
  'Article ' || gs,
  'Content ' || gs,
  (gs - 1) * 5,
  false
FROM generate_series(3, 1000000) gs;

SELECT * FROM tb_articles;

-- Article files
INSERT INTO tb_article_files (file_id, article_id)
SELECT gs, gs FROM generate_series(1, 10000) gs;

-- Article reactions
INSERT INTO tb_article_reactions (user_id, article_id, reaction_type)
SELECT
  ((gs - 1) % 3) + 1,
  gs,
  CASE (gs - 1) % 3
    WHEN 0 THEN 1
    WHEN 1 THEN 0
    ELSE -1
  END
FROM generate_series(1, 10000) gs;

-- Article bookmarks
INSERT INTO tb_article_bookmarks (user_id, article_id)
SELECT
  ((gs - 1) % 3) + 1,
  gs
FROM generate_series(1, 10000) gs;

-- Comments
INSERT INTO tb_comments (
  user_id,
  article_id,
  parent_comment_id,
  root_comment_id,
  depth,
  content
)
SELECT
  ((gs - 1) % 3) + 1,
  gs,
  CASE WHEN gs IN (2, 3) THEN 1 WHEN gs = 4 THEN 2 ELSE NULL END,
  CASE WHEN gs IN (2, 3, 4) THEN 1 ELSE NULL END,
  CASE WHEN gs IN (2, 3) THEN 1 WHEN gs = 4 THEN 2 ELSE 0 END,
  'Comment ' || gs
FROM generate_series(1, 100000) gs;

SELECT * FROM tb_comments;

-- Comment files
INSERT INTO tb_comment_files (file_id, comment_id)
SELECT gs, gs FROM generate_series(1, 10000) gs;

-- Comment reactions
INSERT INTO tb_comment_reactions (user_id, comment_id, reaction_type)
SELECT
  ((gs - 1) % 3) + 1,
  gs,
  CASE (gs - 1) % 3
    WHEN 0 THEN 1
    WHEN 1 THEN 0
    ELSE -1
  END
FROM generate_series(1, 10000) gs;

-- Notifications
INSERT INTO tb_notification (
  user_id,
  sender_id,
  noti_type,
  redirect_url,
  reference_type,
  reference_id,
  is_read
)
SELECT
  ((gs - 1) % 3) + 1,
  CASE (gs - 1) % 3
    WHEN 0 THEN 2
    WHEN 1 THEN 3
    ELSE 1
  END,
  CASE (gs - 1) % 6
    WHEN 0 THEN 'ARTICLE_COMMENT'
    WHEN 1 THEN 'COMMENT_REPLY'
    WHEN 2 THEN 'BOARD_NOTICE'
    WHEN 3 THEN 'SYSTEM'
    WHEN 4 THEN 'REACTION'
    ELSE 'MENTION'
  END,
  '/redirect/' || gs,
  CASE (gs - 1) % 6
    WHEN 0 THEN 'ARTICLE'
    WHEN 1 THEN 'COMMENT'
    WHEN 2 THEN 'BOARD'
    WHEN 3 THEN 'GALLERY'
    WHEN 4 THEN 'FILE'
    ELSE 'USER'
  END,
  gs,
  CASE WHEN gs % 3 = 0 THEN true ELSE false END
FROM generate_series(1, 10000) gs;

-- User files
INSERT INTO tb_user_files (file_id, user_id)
SELECT gs, ((gs - 1) % 3) + 1 FROM generate_series(1, 10000) gs;

-- User OAuth links
INSERT INTO tb_user_oauth_links (user_id, provider, provider_id, email)
SELECT
  ((gs - 1) % 3) + 1,
  'provider' || gs,
  'provider-id-' || gs,
  'oauth' || gs || '@example.com'
FROM generate_series(1, 10000) gs;















































