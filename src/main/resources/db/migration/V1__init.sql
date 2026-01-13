
CREATE TABLE tb_article_bookmarks
(
  article_bookmark_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id             BIGINT      NOT NULL,
  article_id          BIGINT      NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (article_bookmark_id)
);

COMMENT ON TABLE tb_article_bookmarks IS '게시글 북마크, Combination UNIQUE INDEX (user_id, article_id)';

COMMENT ON COLUMN tb_article_bookmarks.article_bookmark_id IS '북마크번호';
COMMENT ON COLUMN tb_article_bookmarks.user_id IS '회원번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_article_bookmarks.article_id IS '글번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_article_bookmarks.created_at IS '생성일시';
COMMENT ON COLUMN tb_article_bookmarks.updated_at IS '수정일시';



CREATE TABLE tb_article_categories
(
  article_category_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  board_id           BIGINT      NOT NULL,
  category_name      VARCHAR(48) NOT NULL,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (article_category_id)
);

COMMENT ON TABLE tb_article_categories IS '게시글 카테고리, Combination UNIQUE INDEX (board_id, category_name)';

COMMENT ON COLUMN tb_article_categories.article_category_id IS '게시글 카테고리 번호';
COMMENT ON COLUMN tb_article_categories.board_id IS '포럼번호';
COMMENT ON COLUMN tb_article_categories.category_name IS '카테고리명';
COMMENT ON COLUMN tb_article_categories.created_at IS '생성일시';
COMMENT ON COLUMN tb_article_categories.updated_at IS '수정일시';



CREATE TABLE tb_article_files
(
  article_file_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_id         BIGINT      NOT NULL,
  article_id      BIGINT      NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (article_file_id)
);

COMMENT ON TABLE tb_article_files IS '게시글-파일 매핑';

COMMENT ON COLUMN tb_article_files.article_file_id IS '매핑 PK';
COMMENT ON COLUMN tb_article_files.file_id IS '첨부파일번호';
COMMENT ON COLUMN tb_article_files.article_id IS '글번호';
COMMENT ON COLUMN tb_article_files.created_at IS '생성일자';
COMMENT ON COLUMN tb_article_files.updated_at IS '수정일자';



CREATE TABLE tb_article_reactions
(
  article_reaction_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id             BIGINT      NOT NULL,
  article_id          BIGINT      NOT NULL,
  reaction_type       SMALLINT    NOT NULL DEFAULT 0,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (article_reaction_id)
);

COMMENT ON TABLE tb_article_reactions IS '게시글 반응, Combination UNIQUE INDEX (user_id, article_id)';

COMMENT ON COLUMN tb_article_reactions.article_reaction_id IS '게시글 반응 번호';
COMMENT ON COLUMN tb_article_reactions.user_id IS '회원번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_article_reactions.article_id IS '글번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_article_reactions.reaction_type IS '리액션 1: 좋아요, -1: 싫어요, 0: 상호작용 기록';
COMMENT ON COLUMN tb_article_reactions.created_at IS '생성일시';
COMMENT ON COLUMN tb_article_reactions.updated_at IS '수정일시';



CREATE TABLE tb_articles
(
  article_id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  board_id            BIGINT       NOT NULL,
  user_id             BIGINT       NOT NULL,
  article_category_id BIGINT      ,
  visibility          VARCHAR(24)  NOT NULL DEFAULT 'PUBLIC',
  title               VARCHAR(255) NOT NULL,
  content             TEXT         NOT NULL,
  hit                 BIGINT       NOT NULL DEFAULT 0,
  is_notice           BOOLEAN      NOT NULL DEFAULT false,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at          TIMESTAMPTZ ,
  PRIMARY KEY (article_id)
);

COMMENT ON TABLE tb_articles IS '게시글';

COMMENT ON COLUMN tb_articles.article_id IS '글번호';
COMMENT ON COLUMN tb_articles.board_id IS '포럼번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_articles.user_id IS '회원번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_articles.article_category_id IS '게시글 카테고리 번호, ON DELETE SET NULL';
COMMENT ON COLUMN tb_articles.visibility IS '공개 범위 PUBLIC, MEMBERS, MANAGERS, ADMINS';
COMMENT ON COLUMN tb_articles.title IS '글제목';
COMMENT ON COLUMN tb_articles.content IS '글내용';
COMMENT ON COLUMN tb_articles.hit IS '조회수';
COMMENT ON COLUMN tb_articles.is_notice IS '공지글 여부';
COMMENT ON COLUMN tb_articles.created_at IS '생성일자';
COMMENT ON COLUMN tb_articles.updated_at IS '수정일자';
COMMENT ON COLUMN tb_articles.deleted_at IS '삭제일자';



CREATE TABLE tb_board
(
  board_id    BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  board_name  VARCHAR(255) NOT NULL,
  slug        VARCHAR(80)  NOT NULL,
  description TEXT        ,
  visibility  VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC',
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at  TIMESTAMPTZ ,
  PRIMARY KEY (board_id)
);

COMMENT ON TABLE tb_board IS '커뮤니티 - 게시판 주제 or 커뮤니티';

COMMENT ON COLUMN tb_board.board_id IS '게시판번호';
COMMENT ON COLUMN tb_board.board_name IS '게시판명';
COMMENT ON COLUMN tb_board.slug IS 'url 용 (예: /b/spring)';
COMMENT ON COLUMN tb_board.description IS '게시판 설명';
COMMENT ON COLUMN tb_board.visibility IS 'PUBLIC/GROUP/PRIVATE/UNLISTED';
COMMENT ON COLUMN tb_board.created_at IS '생성일자';
COMMENT ON COLUMN tb_board.updated_at IS '수정일자';
COMMENT ON COLUMN tb_board.deleted_at IS '삭제여부';



CREATE TABLE tb_board_files
(
  board_file_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_id       BIGINT      NOT NULL,
  board_id      BIGINT      NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (board_file_id)
);

COMMENT ON TABLE tb_board_files IS '커뮤니티-파일 매핑 (배너이미지, 대표이미지 등등)';

COMMENT ON COLUMN tb_board_files.board_file_id IS '매핑 PK';
COMMENT ON COLUMN tb_board_files.file_id IS '첨부파일번호';
COMMENT ON COLUMN tb_board_files.board_id IS '게시판번호';
COMMENT ON COLUMN tb_board_files.created_at IS '생성일시';
COMMENT ON COLUMN tb_board_files.updated_at IS '수정일시';



CREATE TABLE tb_board_members
(
  board_manager_id   BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id            BIGINT      NOT NULL,
  board_id           BIGINT      NOT NULL,
  granted_by_user_id BIGINT     ,
  board_role         VARCHAR(24) NOT NULL,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (board_manager_id)
);

COMMENT ON TABLE tb_board_members IS '커뮤니티 멤버';

COMMENT ON COLUMN tb_board_members.board_manager_id IS '게시판 멤버 번호';
COMMENT ON COLUMN tb_board_members.user_id IS '회원번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_board_members.board_id IS '포럼번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_board_members.granted_by_user_id IS '누가 부여했는지';
COMMENT ON COLUMN tb_board_members.board_role IS '관리자 및 멤버(일반유저), CHECK (OWNER, MODERATOR, MEMBER, BANNED)';
COMMENT ON COLUMN tb_board_members.created_at IS '생성일자';
COMMENT ON COLUMN tb_board_members.updated_at IS '수정일자';



CREATE TABLE tb_board_subscribes
(
  board_subscribe_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id            BIGINT      NOT NULL,
  board_id           BIGINT      NOT NULL,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (board_subscribe_id)
);

COMMENT ON TABLE tb_board_subscribes IS '커뮤니티 구독 목록, Combination UNIQUE INDEX (member_id, forum_id)';

COMMENT ON COLUMN tb_board_subscribes.board_subscribe_id IS '구독목록번호';
COMMENT ON COLUMN tb_board_subscribes.user_id IS '회원번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_board_subscribes.board_id IS '커뮤니티번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_board_subscribes.created_at IS '구독일시';
COMMENT ON COLUMN tb_board_subscribes.updated_at IS '수정일';



CREATE TABLE tb_comment_files
(
  comment_file_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_id         BIGINT      NOT NULL,
  comment_id      BIGINT      NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (comment_file_id)
);

COMMENT ON TABLE tb_comment_files IS '댓글-파일 매핑 (댓글이모티콘용)';

COMMENT ON COLUMN tb_comment_files.comment_file_id IS '매핑 PK';
COMMENT ON COLUMN tb_comment_files.file_id IS '첨부파일번호';
COMMENT ON COLUMN tb_comment_files.comment_id IS '댓글번호';
COMMENT ON COLUMN tb_comment_files.created_at IS '생성일자';
COMMENT ON COLUMN tb_comment_files.updated_at IS '수정일자';



CREATE TABLE tb_comment_reactions
(
  comment_reaction_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id             BIGINT      NOT NULL,
  comment_id          BIGINT      NOT NULL,
  reaction_type       SMALLINT    NOT NULL DEFAULT 0,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (comment_reaction_id)
);

COMMENT ON TABLE tb_comment_reactions IS '댓글 좋아요, Combination UNIQUE INDEX (user_id, comment_id)';

COMMENT ON COLUMN tb_comment_reactions.comment_reaction_id IS '댓글 좋아요 번호';
COMMENT ON COLUMN tb_comment_reactions.user_id IS '회원번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_comment_reactions.comment_id IS '댓글번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_comment_reactions.reaction_type IS '리액션 1: 좋아요, -1: 싫어요, 0: 상호작용 기록';
COMMENT ON COLUMN tb_comment_reactions.created_at IS '생성일자';
COMMENT ON COLUMN tb_comment_reactions.updated_at IS '수정일자';



CREATE TABLE tb_comments
(
  comment_id        BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id           BIGINT      NOT NULL,
  article_id        BIGINT      NOT NULL,
  parent_comment_id BIGINT     ,
  root_comment_id   BIGINT     ,
  depth             INT         NOT NULL DEFAULT 0,
  content           TEXT        NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ,
  PRIMARY KEY (comment_id)
);

COMMENT ON TABLE tb_comments IS '댓글';

COMMENT ON COLUMN tb_comments.comment_id IS '댓글번호';
COMMENT ON COLUMN tb_comments.user_id IS '댓글 작성자, ON DELETE CASCADE';
COMMENT ON COLUMN tb_comments.article_id IS '게시글번호, ON DELETE CASCADE';
COMMENT ON COLUMN tb_comments.parent_comment_id IS '댓글 계층 (부모id - comment_id), ON DELETE CASCADE';
COMMENT ON COLUMN tb_comments.root_comment_id IS '루트 댓글 (뿌리id - comment_id), ON DELETE CASCADE';
COMMENT ON COLUMN tb_comments.depth IS '0=루트, 1=대댓글, 2=대대댓, ...';
COMMENT ON COLUMN tb_comments.content IS '댓글내용';
COMMENT ON COLUMN tb_comments.created_at IS '작성일자';
COMMENT ON COLUMN tb_comments.updated_at IS '수정일자';
COMMENT ON COLUMN tb_comments.deleted_at IS '삭제일자';



CREATE TABLE tb_file_classes
(
  file_class_id   BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_class_code VARCHAR(32)  NOT NULL,
  file_class_name VARCHAR(64)  NOT NULL,
  description     VARCHAR(255),
  media_kind      VARCHAR(16)  NOT NULL,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ ,
  PRIMARY KEY (file_class_id)
);

COMMENT ON TABLE tb_file_classes IS '파일 분류(카테고리/정책) 마스터 테이블';

COMMENT ON COLUMN tb_file_classes.file_class_id IS '파일분류번호';
COMMENT ON COLUMN tb_file_classes.file_class_code IS '파일분류코드';
COMMENT ON COLUMN tb_file_classes.file_class_name IS '파일 분류명';
COMMENT ON COLUMN tb_file_classes.description IS '설명';
COMMENT ON COLUMN tb_file_classes.media_kind IS '미디어 종류(예: image/video/audio/document 등)';
COMMENT ON COLUMN tb_file_classes.created_at IS '생성시각';
COMMENT ON COLUMN tb_file_classes.updated_at IS '수정시각';
COMMENT ON COLUMN tb_file_classes.deleted_at IS '삭제시각';



CREATE TABLE tb_files
(
  file_id       BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_class_id BIGINT        NOT NULL,
  file_name     VARCHAR(255)  NOT NULL,
  storage_key   VARCHAR(1024) NOT NULL,
  file_size     BIGINT        NOT NULL,
  mime_type     VARCHAR(128)   NOT NULL,
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ  ,
  PRIMARY KEY (file_id)
);

COMMENT ON TABLE tb_files IS '파일 메타데이터(저장소 키 기반) 테이블';

COMMENT ON COLUMN tb_files.file_id IS '첨부파일번호';
COMMENT ON COLUMN tb_files.file_class_id IS '파일분류번호';
COMMENT ON COLUMN tb_files.file_name IS '원본 파일명';
COMMENT ON COLUMN tb_files.storage_key IS '실제 저장소 키/경로';
COMMENT ON COLUMN tb_files.file_size IS '파일 크기 byte';
COMMENT ON COLUMN tb_files.mime_type IS 'MIME 타입(예: image/png)';
COMMENT ON COLUMN tb_files.created_at IS '생성일자';
COMMENT ON COLUMN tb_files.updated_at IS '수정일자';
COMMENT ON COLUMN tb_files.deleted_at IS '삭제시각';



CREATE TABLE tb_notification
(
  notification_id BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id         BIGINT        NOT NULL,
  sender_id       BIGINT       ,
  noti_type       VARCHAR(20)   NOT NULL,
  redirect_url    VARCHAR(1024),
  reference_type  VARCHAR(24)   NOT NULL,
  reference_id    BIGINT        NOT NULL,
  is_read         BOOLEAN       NOT NULL DEFAULT false,
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  PRIMARY KEY (notification_id)
);

COMMENT ON TABLE tb_notification IS '알림 - 게시글 or 댓글 푸쉬 알림';

COMMENT ON COLUMN tb_notification.notification_id IS '알림번호';
COMMENT ON COLUMN tb_notification.user_id IS '회원번호 - 알림 수신자, ON DELETE CASCADE';
COMMENT ON COLUMN tb_notification.sender_id IS '발신자 member_id, ON DELETE CASCADE';
COMMENT ON COLUMN tb_notification.noti_type IS '알림유형';
COMMENT ON COLUMN tb_notification.redirect_url IS '바로가기';
COMMENT ON COLUMN tb_notification.reference_type IS '참조 구분';
COMMENT ON COLUMN tb_notification.reference_id IS '참조 id';
COMMENT ON COLUMN tb_notification.is_read IS '읽음 여부';
COMMENT ON COLUMN tb_notification.created_at IS '생성일자';
COMMENT ON COLUMN tb_notification.updated_at IS '수정일자';



CREATE TABLE tb_role
(
  role_id     BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  role_name   VARCHAR(24) NOT NULL,
  auth_bit    INT         NOT NULL,
  description VARCHAR(36),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (role_id)
);

COMMENT ON TABLE tb_role IS '역할(권한) 마스터 테이블';

COMMENT ON COLUMN tb_role.role_id IS '권한번호';
COMMENT ON COLUMN tb_role.role_name IS '권한 또는 역할명';
COMMENT ON COLUMN tb_role.auth_bit IS '권한비트';
COMMENT ON COLUMN tb_role.description IS '역할 설명';
COMMENT ON COLUMN tb_role.created_at IS '생성일자';
COMMENT ON COLUMN tb_role.updated_at IS '수정일자';

INSERT INTO tb_role (role_name, auth_bit, description)
VALUES
  ('USER', 1, '기본 사용자(읽기)'),
  ('WRITER', 3, '작성 가능(읽기+쓰기)'),
  ('MANAGER', 7, '매니저(읽기+쓰기+삭제)'),
  ('ADMIN', 15, '전체 권한');



CREATE TABLE tb_user_files
(
  user_file_id BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_id      BIGINT      NOT NULL,
  user_id      BIGINT      NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_file_id)
);

COMMENT ON TABLE tb_user_files IS '사용자-파일 매핑(소유/첨부 관계) 테이블';

COMMENT ON COLUMN tb_user_files.user_file_id IS '매핑 PK';
COMMENT ON COLUMN tb_user_files.file_id IS '첨부파일번호';
COMMENT ON COLUMN tb_user_files.user_id IS '회원번호';
COMMENT ON COLUMN tb_user_files.created_at IS '생성일시';
COMMENT ON COLUMN tb_user_files.updated_at IS '수정일시';



CREATE TABLE tb_user_oauth_links
(
  user_oauth_link_id BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id            BIGINT       NOT NULL,
  provider           VARCHAR(24)  NOT NULL,
  provider_id        VARCHAR(128) NOT NULL,
  email              VARCHAR(128),
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  PRIMARY KEY (user_oauth_link_id)
);

COMMENT ON TABLE tb_user_oauth_links IS '소셜로그인제공자';

COMMENT ON COLUMN tb_user_oauth_links.user_oauth_link_id IS '소셜로그인제공자번호';
COMMENT ON COLUMN tb_user_oauth_links.user_id IS '회원번호';
COMMENT ON COLUMN tb_user_oauth_links.provider IS '제공자';
COMMENT ON COLUMN tb_user_oauth_links.provider_id IS 'OAuth 제공자 아이디';
COMMENT ON COLUMN tb_user_oauth_links.email IS '이메일';
COMMENT ON COLUMN tb_user_oauth_links.created_at IS '생성일자';
COMMENT ON COLUMN tb_user_oauth_links.updated_at IS '수정일자';



CREATE TABLE tb_users
(
  user_id           BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  role_id           BIGINT       NOT NULL,
  login_id          VARCHAR(128) NOT NULL,
  email             VARCHAR(128) NOT NULL,
  pw_hash           VARCHAR(255) NOT NULL,
  user_name         VARCHAR(32)  NOT NULL,
  display_name      VARCHAR(16)  NOT NULL,
  handle            VARCHAR(24)  NOT NULL,
  user_point        INT          NOT NULL DEFAULT 0,
  is_email_verified BOOLEAN      NOT NULL DEFAULT false,
  is_enabled        BOOLEAN      NOT NULL DEFAULT true,
  is_locked         BOOLEAN      NOT NULL DEFAULT false,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ ,
  PRIMARY KEY (user_id)
);

COMMENT ON TABLE tb_users IS '회원';

COMMENT ON COLUMN tb_users.user_id IS '회원번호';
COMMENT ON COLUMN tb_users.role_id IS '권한번호, ON DELETE RESTRICT';
COMMENT ON COLUMN tb_users.login_id IS '회원 아이디 ';
COMMENT ON COLUMN tb_users.email IS '이메일';
COMMENT ON COLUMN tb_users.pw_hash IS '회원 비밀번호 (argon2 해쉬 사용)';
COMMENT ON COLUMN tb_users.user_name IS '이름(실명)';
COMMENT ON COLUMN tb_users.display_name IS '닉네임';
COMMENT ON COLUMN tb_users.handle IS '핸들';
COMMENT ON COLUMN tb_users.user_point IS '포인트';
COMMENT ON COLUMN tb_users.is_email_verified IS '이메일 인증 여부';
COMMENT ON COLUMN tb_users.is_enabled IS '계정 활성 여부';
COMMENT ON COLUMN tb_users.is_locked IS '계정 감금 여부';
COMMENT ON COLUMN tb_users.created_at IS '가입일자';
COMMENT ON COLUMN tb_users.updated_at IS '수정일자';
COMMENT ON COLUMN tb_users.deleted_at IS '탈퇴여부';



ALTER TABLE tb_articles
  ADD CONSTRAINT fk_tb_articles_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_comments
  ADD CONSTRAINT fk_tb_comments_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_comments
  ADD CONSTRAINT fk_tb_comments_article_id__tb_articles
    FOREIGN KEY (article_id)
    REFERENCES tb_articles (article_id);

ALTER TABLE tb_board_members
  ADD CONSTRAINT fk_tb_board_members_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_users
  ADD CONSTRAINT fk_tb_users_role_id__tb_role
    FOREIGN KEY (role_id)
    REFERENCES tb_role (role_id);

ALTER TABLE tb_article_reactions
  ADD CONSTRAINT fk_tb_article_reactions_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_article_reactions
  ADD CONSTRAINT fk_tb_article_reactions_article_id__tb_articles
    FOREIGN KEY (article_id)
    REFERENCES tb_articles (article_id);

ALTER TABLE tb_articles
  ADD CONSTRAINT fk_tb_articles_board_id__tb_board
    FOREIGN KEY (board_id)
    REFERENCES tb_board (board_id);

ALTER TABLE tb_board_members
  ADD CONSTRAINT fk_tb_board_members_board_id__tb_board
    FOREIGN KEY (board_id)
    REFERENCES tb_board (board_id);

ALTER TABLE tb_board_subscribes
  ADD CONSTRAINT fk_tb_board_subscribes_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_board_subscribes
  ADD CONSTRAINT fk_tb_board_subscribes_board_id__tb_board
    FOREIGN KEY (board_id)
    REFERENCES tb_board (board_id);

ALTER TABLE tb_article_bookmarks
  ADD CONSTRAINT fk_tb_article_bookmarks_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_article_bookmarks
  ADD CONSTRAINT fk_tb_article_bookmarks_article_id__tb_articles
    FOREIGN KEY (article_id)
    REFERENCES tb_articles (article_id);

ALTER TABLE tb_comments
  ADD CONSTRAINT fk_tb_comments_parent_comment_id__tb_comments
    FOREIGN KEY (parent_comment_id)
    REFERENCES tb_comments (comment_id);

ALTER TABLE tb_notification
  ADD CONSTRAINT fk_tb_notification_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_notification
  ADD CONSTRAINT fk_tb_notification_sender_id__tb_users
    FOREIGN KEY (sender_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_comment_reactions
  ADD CONSTRAINT fk_tb_comment_reactions_comment_id__tb_comments
    FOREIGN KEY (comment_id)
    REFERENCES tb_comments (comment_id);

ALTER TABLE tb_comment_reactions
  ADD CONSTRAINT fk_tb_comment_reactions_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_article_categories
  ADD CONSTRAINT fk_tb_article_categories_board_id__tb_board
    FOREIGN KEY (board_id)
    REFERENCES tb_board (board_id);

ALTER TABLE tb_articles
  ADD CONSTRAINT fk_tb_articles_article_category_id__tb_article_categories
    FOREIGN KEY (article_category_id)
    REFERENCES tb_article_categories (article_category_id);

ALTER TABLE tb_user_oauth_links
  ADD CONSTRAINT fk_tb_user_oauth_links_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_files
  ADD CONSTRAINT fk_tb_files_file_class_id__tb_file_classes
    FOREIGN KEY (file_class_id)
    REFERENCES tb_file_classes (file_class_id);

ALTER TABLE tb_user_files
  ADD CONSTRAINT fk_tb_user_files_file_id__tb_files
    FOREIGN KEY (file_id)
    REFERENCES tb_files (file_id);

ALTER TABLE tb_user_files
  ADD CONSTRAINT fk_tb_user_files_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_comments
  ADD CONSTRAINT fk_tb_comments_root_comment_id__tb_comments
    FOREIGN KEY (root_comment_id)
    REFERENCES tb_comments (comment_id);

ALTER TABLE tb_article_files
  ADD CONSTRAINT fk_tb_article_files_article_id__tb_articles
    FOREIGN KEY (article_id)
    REFERENCES tb_articles (article_id);

ALTER TABLE tb_article_files
  ADD CONSTRAINT fk_tb_article_files_file_id__tb_files
    FOREIGN KEY (file_id)
    REFERENCES tb_files (file_id);

ALTER TABLE tb_board_members
  ADD CONSTRAINT fk_tb_board_members_granted_by_user_id__tb_users
    FOREIGN KEY (granted_by_user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_comment_files
  ADD CONSTRAINT fk_tb_comment_files_file_id__tb_files
    FOREIGN KEY (file_id)
    REFERENCES tb_files (file_id);

ALTER TABLE tb_comment_files
  ADD CONSTRAINT fk_tb_comment_files_comment_id__tb_comments
    FOREIGN KEY (comment_id)
    REFERENCES tb_comments (comment_id);

ALTER TABLE tb_board_files
  ADD CONSTRAINT fk_tb_board_files_file_id__tb_files
    FOREIGN KEY (file_id)
    REFERENCES tb_files (file_id);

ALTER TABLE tb_board_files
  ADD CONSTRAINT fk_tb_board_files_board_id__tb_board
    FOREIGN KEY (board_id)
    REFERENCES tb_board (board_id);

ALTER TABLE tb_board
  ADD CONSTRAINT uq_tb_board_board_name UNIQUE (board_name);

ALTER TABLE tb_board
  ADD CONSTRAINT uq_tb_board_slug UNIQUE (slug);

ALTER TABLE tb_users
  ADD CONSTRAINT uq_tb_users_login_id UNIQUE (login_id);

ALTER TABLE tb_users
  ADD CONSTRAINT uq_tb_users_email UNIQUE (email);

ALTER TABLE tb_users
  ADD CONSTRAINT uq_tb_users_handle UNIQUE (handle);

ALTER TABLE tb_board_members
  ADD CONSTRAINT uq_tb_board_members_user_id_board_id UNIQUE (user_id, board_id);

ALTER TABLE tb_board_subscribes
  ADD CONSTRAINT uq_tb_board_subscribes_user_id_board_id UNIQUE (user_id, board_id);

ALTER TABLE tb_article_bookmarks
  ADD CONSTRAINT uq_tb_article_bookmarks_user_id_article_id UNIQUE (user_id, article_id);

ALTER TABLE tb_article_reactions
  ADD CONSTRAINT uq_tb_article_reactions_user_id_article_id UNIQUE (user_id, article_id);

ALTER TABLE tb_comment_reactions
  ADD CONSTRAINT uq_tb_comment_reactions_user_id_comment_id UNIQUE (user_id, comment_id);

ALTER TABLE tb_article_categories
  ADD CONSTRAINT uq_tb_article_categories_board_id_category_name UNIQUE (board_id, category_name);

ALTER TABLE tb_user_oauth_links
  ADD CONSTRAINT uq_tb_user_oauth_links_provider_provider_id UNIQUE (provider, provider_id);

ALTER TABLE tb_user_oauth_links
  ADD CONSTRAINT uq_tb_user_oauth_links_user_id_provider UNIQUE (user_id, provider);

ALTER TABLE tb_article_files
  ADD CONSTRAINT uq_tb_article_files_article_id_file_id UNIQUE (article_id, file_id);

ALTER TABLE tb_comment_files
  ADD CONSTRAINT uq_tb_comment_files_comment_id_file_id UNIQUE (comment_id, file_id);

ALTER TABLE tb_board_files
  ADD CONSTRAINT uq_tb_board_files_board_id_file_id UNIQUE (board_id, file_id);

ALTER TABLE tb_user_files
  ADD CONSTRAINT uq_tb_user_files_user_id_file_id UNIQUE (user_id, file_id);

ALTER TABLE tb_files
  ADD CONSTRAINT uq_tb_files_storage_key UNIQUE (storage_key);

ALTER TABLE tb_role
  ADD CONSTRAINT uq_tb_role_role_name UNIQUE (role_name);


CREATE INDEX IF NOT EXISTS ix_tb_articles_board_id_created_at
  ON tb_articles (board_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tb_comments_article_id_created_at
  ON tb_comments (article_id, created_at)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tb_comments_parent_comment_id
  ON tb_comments (parent_comment_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tb_comments_root_comment_id
  ON tb_comments (root_comment_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tb_notification_user_id_is_read_created_at
  ON tb_notification (user_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_article_reactions_article_id
  ON tb_article_reactions (article_id);

CREATE INDEX IF NOT EXISTS ix_tb_comment_reactions_comment_id
  ON tb_comment_reactions (comment_id);

CREATE INDEX IF NOT EXISTS ix_tb_article_bookmarks_article_id
  ON tb_article_bookmarks (article_id);

CREATE INDEX IF NOT EXISTS ix_tb_articles_user_id_created_at
  ON tb_articles (user_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tb_comments_user_id_created_at
  ON tb_comments (user_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tb_board_members_board_id
  ON tb_board_members (board_id);

CREATE INDEX IF NOT EXISTS ix_tb_board_subscribes_board_id
  ON tb_board_subscribes (board_id);

CREATE INDEX IF NOT EXISTS ix_tb_article_files_file_id
  ON tb_article_files (file_id);

CREATE INDEX IF NOT EXISTS ix_tb_comment_files_file_id
  ON tb_comment_files (file_id);

CREATE INDEX IF NOT EXISTS ix_tb_board_files_file_id
  ON tb_board_files (file_id);

CREATE INDEX IF NOT EXISTS ix_tb_user_files_file_id
  ON tb_user_files (file_id);

CREATE INDEX IF NOT EXISTS ix_tb_article_files_article_id
  ON tb_article_files (article_id);

CREATE INDEX IF NOT EXISTS ix_tb_comment_files_comment_id
  ON tb_comment_files (comment_id);

CREATE INDEX IF NOT EXISTS ix_tb_board_files_board_id
  ON tb_board_files (board_id);

CREATE INDEX IF NOT EXISTS ix_tb_user_files_file_id
  ON tb_user_files (file_id);

CREATE INDEX IF NOT EXISTS ix_tb_users_role_id
  ON tb_users (role_id);
