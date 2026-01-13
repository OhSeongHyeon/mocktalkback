-- Dummy data for mocktalk (minimum 10 rows per table, except tb_role/tb_users)
-- Assumes a clean database with identity sequences starting at 1.

-- Roles
INSERT INTO tb_role (role_name, auth_bit, description)
VALUES
  ('USER', 1, '기본 사용자(읽기)'),
  ('WRITER', 3, '작성 가능(읽기+쓰기)'),
  ('MANAGER', 7, '모더레이터(읽기+쓰기+삭제)'),
  ('ADMIN', 15, '전체 권한');

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
   'Seed User', 'SeedUser', 'seed_user', 0, false, true, false),
  ((SELECT role_id FROM tb_role WHERE role_name = 'WRITER'),
   'seed_writer', 'seed_writer@example.com', '$argon2id$v=19$m=16384,t=2,p=1$MBgerTYHMKeCFXqevHRcPw$nrbsGsSKsslFN/W1K5/322yZlbPM7R70tWtvpeyb9dQ',
   'Seed Writer', 'SeedWriter', 'seed_writer', 0, false, true, false),
  ((SELECT role_id FROM tb_role WHERE role_name = 'MANAGER'),
   'seed_moderator', 'seed_moderator@example.com', '$argon2id$v=19$m=16384,t=2,p=1$k0FBG6+eySQ1LuOVrjQJdg$5euufr9CBtbjHhS7x7D/mpvOR9y/yrIKP06w3jHDQh4',
   'Seed Moderator', 'SeedMod', 'seed_moderator', 0, false, true, false),
  ((SELECT role_id FROM tb_role WHERE role_name = 'ADMIN'),
   'seed_admin', 'seed_admin@example.com', '$argon2id$v=19$m=16384,t=2,p=1$h3R1ST6MeVCTl8O0okEVRw$FIY6OdQ5Mm8PPRHS8gZInLqivnhcukBYUhBjMfcxVK8',
   'Seed Admin', 'SeedAdmin', 'seed_admin', 0, false, true, false);

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
INSERT INTO tb_board (board_name, slug, description, visibility)
SELECT
  'Board ' || gs,
  'board-' || gs,
  'Board desc ' || gs,
  CASE (gs - 1) % 4
    WHEN 0 THEN 'PUBLIC'
    WHEN 1 THEN 'MEMBERS'
    WHEN 2 THEN 'MANAGERS'
    ELSE 'ADMINS'
  END
FROM generate_series(1, 10000) gs;

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
  gs,
  ((gs - 1) % 3) + 1,
  gs,
  CASE (gs - 1) % 4
    WHEN 0 THEN 'PUBLIC'
    WHEN 1 THEN 'MEMBERS'
    WHEN 2 THEN 'MANAGERS'
    ELSE 'ADMINS'
  END,
  'Article ' || gs,
  'Content ' || gs,
  (gs - 1) * 5,
  CASE WHEN gs = 1 THEN true ELSE false END
FROM generate_series(1, 10000) gs;

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
FROM generate_series(1, 10000) gs;

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
