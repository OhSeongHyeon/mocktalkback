-- =================================
-- mocktalk project: table ddl
-- mocktalk_ddl.sql
-- 테이블 스키마
-- =================================

DROP DATABASE IF EXISTS mocktalk;
CREATE DATABASE mocktalk;
USE mocktalk;


CREATE TABLE tb_attachment_file (
    attachment_file_id BIGINT        NOT NULL AUTO_INCREMENT COMMENT '첨부파일번호',
    thread_id          BIGINT        NOT NULL COMMENT '글번호, ON DELETE CASCADE',
    file_name          VARCHAR(255)  NOT NULL COMMENT '실제 파일명',
    file_path          VARCHAR(1024) NOT NULL COMMENT '저장경로',
    file_uuid          VARCHAR(36)   NOT NULL COMMENT 'UUID, 디스크 저장된 파일명',
    file_size          BIGINT        NOT NULL COMMENT '파일 크기 byte',
    file_type          VARCHAR(96)   NOT NULL COMMENT 'MIME Type - img, png, etc...',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (attachment_file_id)
) COMMENT '첨부파일';

ALTER TABLE tb_attachment_file ADD CONSTRAINT UQ_file_name UNIQUE (file_name);
ALTER TABLE tb_attachment_file ADD CONSTRAINT UQ_file_uuid UNIQUE (file_uuid);


CREATE TABLE tb_forum (
    forum_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '포럼번호',
    forum_name   VARCHAR(255) NOT NULL COMMENT '포럼명',
    forum_code   VARCHAR(255) NOT NULL COMMENT '포럼코드, 포럼핸들',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (forum_id)
) COMMENT '포럼 - 게시판 주제 or 커뮤니티';

ALTER TABLE tb_forum ADD CONSTRAINT UQ_forum_name UNIQUE (forum_name);
ALTER TABLE tb_forum ADD CONSTRAINT UQ_forum_code UNIQUE (forum_code);


CREATE TABLE tb_forum_manager (
    forum_manager_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '포럼 관리자 번호',
    member_id        BIGINT      NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    forum_id         BIGINT      NOT NULL COMMENT '포럼번호, ON DELETE CASCADE',
    fm_role          VARCHAR(20) NOT NULL COMMENT '관리자등급, 권한 비교 크기, CHECK (manager, assistant)',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (forum_manager_id)
) COMMENT '포럼 관리자, Combination UNIQUE INDEX (member_id, forum_id)';


CREATE TABLE tb_forum_subscribe (
    forum_subscribe_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '구독목록번호',
    member_id          BIGINT     NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    forum_id           BIGINT     NOT NULL COMMENT '포럼번호, ON DELETE CASCADE',
    created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '구독일시',
    updated_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    deleted_flag       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (forum_subscribe_id)
) COMMENT '커뮤니티 구독 목록, Combination UNIQUE INDEX (member_id, forum_id)';


CREATE TABLE tb_hate_reply (
    hate_reply_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '댓글 싫어요 번호',
    member_id     BIGINT     NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    reply_id      BIGINT     NOT NULL COMMENT '댓글번호, ON DELETE CASCADE',
    created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag  TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (hate_reply_id)
) COMMENT '댓글 싫어요, Combination UNIQUE INDEX (member_id, reply_id)';


CREATE TABLE tb_hate_thread (
    hate_thread_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '싫어요번호',
    member_id      BIGINT     NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    thread_id      BIGINT     NOT NULL COMMENT '글번호, ON DELETE CASCADE',
    created_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_flag   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (hate_thread_id)
) COMMENT '게시글 싫어요, Combination UNIQUE INDEX (member_id, thread_id)';


CREATE TABLE tb_like_reply (
    like_reply_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '댓글 좋아요 번호',
    member_id     BIGINT     NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    reply_id      BIGINT     NOT NULL COMMENT '댓글번호, ON DELETE CASCADE',
    created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag  TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (like_reply_id)
) COMMENT '댓글 좋아요, Combination UNIQUE INDEX (member_id, reply_id)';


CREATE TABLE tb_like_thread (
    like_thread_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '좋아요번호',
    member_id      BIGINT     NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    thread_id      BIGINT     NOT NULL COMMENT '글번호, ON DELETE CASCADE',
    created_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_flag   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (like_thread_id)
) COMMENT '게시글 좋아요, Combination UNIQUE INDEX (member_id, thread_id)';


CREATE TABLE tb_member (
    member_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원번호',
    role_id          BIGINT       NOT NULL COMMENT '권한번호, ON DELETE RESTRICT',
    profile_image_id BIGINT       NULL     COMMENT '프로필이미지번호, ON DELETE SET NULL',
    login_id         VARCHAR(64)  NOT NULL COMMENT '회원 아이디',
    login_pw_hash    VARCHAR(60)  NOT NULL COMMENT '회원 비밀번호 (bcrypt 해쉬 사용)',
    member_name      VARCHAR(32)  NOT NULL COMMENT '이름(실명)',
    nick_name        VARCHAR(16)  NOT NULL COMMENT '닉네임 중복허용안함, 16자까지 허용',
    email            VARCHAR(128) NOT NULL COMMENT '이메일',
    member_point     INT          NOT NULL DEFAULT 0 COMMENT '포인트',
    is_social_id     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '소셜아이디여부',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일자',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '탈퇴여부',
    PRIMARY KEY (member_id)
) COMMENT '회원';

ALTER TABLE tb_member ADD CONSTRAINT UQ_login_id UNIQUE (login_id);
ALTER TABLE tb_member ADD CONSTRAINT UQ_nick_name UNIQUE (nick_name);
ALTER TABLE tb_member ADD CONSTRAINT UQ_email UNIQUE (email);


CREATE TABLE tb_notification (
    notification_id BIGINT        NOT NULL AUTO_INCREMENT COMMENT '알림번호',
    member_id       BIGINT        NOT NULL COMMENT '회원번호 - 알림 수신자, ON DELETE CASCADE',
    sender_id       BIGINT        NULL     COMMENT '발신자 member_id, ON DELETE CASCADE',
    noti_type       VARCHAR(20)   NULL     COMMENT '알림유형, CHECK (like, mention, board, reply, notice)',
    redirect_url    VARCHAR(1024) NULL     COMMENT '바로가기',
    reference_type  VARCHAR(5)    NOT NULL COMMENT '참조 구분 CHECK (board, reply)',
    reference_id    BIGINT        NOT NULL COMMENT '참조 id - FK(thread_id, reply_id)',
    is_read         TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '읽음 여부',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (notification_id)
) COMMENT '알림 - 게시글 or 댓글 푸쉬 알림';


CREATE TABLE tb_profile_image (
    profile_image_id BIGINT        NOT NULL AUTO_INCREMENT COMMENT '프로필이미지번호',
    file_name        VARCHAR(255)  NOT NULL COMMENT '원본 파일명',
    file_path        VARCHAR(1024) NOT NULL COMMENT '저장경로',
    file_uuid        VARCHAR(36)   NOT NULL COMMENT 'UUID, 디스크 저장된 파일명',
    file_size        BIGINT        NOT NULL COMMENT '파일 크기 byte',
    file_type        VARCHAR(96)   NOT NULL COMMENT 'MIME Type - img, png, etc...',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (profile_image_id)
) COMMENT '프로필이미지';

ALTER TABLE tb_profile_image ADD CONSTRAINT UQ_file_name UNIQUE (file_name);
ALTER TABLE tb_profile_image ADD CONSTRAINT UQ_file_uuid UNIQUE (file_uuid);


CREATE TABLE tb_reply (
    reply_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '댓글번호',
    member_id    BIGINT       NOT NULL COMMENT '댓글 작성자, ON DELETE CASCADE',
    thread_id    BIGINT       NOT NULL COMMENT '게시글번호, ON DELETE CASCADE',
    parent_id    BIGINT       NULL     COMMENT '댓글 계층 (부모id - reply_id), ON DELETE CASCADE',
    content      VARCHAR(500) NOT NULL COMMENT '댓글내용 (500자 제한)',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일자',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (reply_id)
) COMMENT '댓글';


CREATE TABLE tb_role (
    role_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '권한번호',
    role_name    VARCHAR(16) NOT NULL COMMENT '권한 또는 역할명',
    auth_lv      INT         NOT NULL DEFAULT 0 COMMENT '높을 수록 강한 권한',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (role_id)
) COMMENT '권한 또는 역할 - admin, teacher, mentor, student, member';

ALTER TABLE tb_role ADD CONSTRAINT UQ_role_name UNIQUE (role_name);
ALTER TABLE tb_role ADD CONSTRAINT UQ_auth_lv UNIQUE (auth_lv);


CREATE TABLE tb_thread (
    thread_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '글번호',
    forum_id           BIGINT       NOT NULL COMMENT '포럼번호, ON DELETE CASCADE',
    member_id          BIGINT       NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    role_id            BIGINT       NOT NULL COMMENT '권한번호 (해당 글을 볼 수 있는 권한), ON DELETE RESTRICT',
    thread_category_id BIGINT       NULL     COMMENT '게시글 카테고리 번호, ON DELETE SET NULL',
    title              VARCHAR(255) NOT NULL COMMENT '글제목',
    content            TEXT         NOT NULL COMMENT '글내용',
    is_publish         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '임시저장 or 글노출 여부 - 1 노출, 0 숨기기',
    hit                BIGINT       NOT NULL DEFAULT 0 COMMENT '조회수',
    is_notice          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '공지글 여부',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일자',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    deleted_flag       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (thread_id)
) COMMENT '게시글';


CREATE TABLE tb_thread_bookmark (
    thread_bookmark_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '북마크번호',
    member_id          BIGINT     NOT NULL COMMENT '회원번호, ON DELETE CASCADE',
    thread_id          BIGINT     NOT NULL COMMENT '글번호, ON DELETE CASCADE',
    created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_flag       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (thread_bookmark_id)
) COMMENT '게시글 북마크, Combination UNIQUE INDEX (member_id, thread_id)';


CREATE TABLE tb_thread_category (
    thread_category_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '게시글 카테고리 번호',
    forum_id           BIGINT      NOT NULL COMMENT '포럼번호',
    category_name      VARCHAR(32) NOT NULL COMMENT '카테고리명',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_flag       TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '삭제여부',
    PRIMARY KEY (thread_category_id)
) COMMENT '게시글 카테고리, Combination UNIQUE INDEX (forum_id, category_name)';


-- FK CONSTRAINT =================================================
ALTER TABLE tb_thread ADD CONSTRAINT FK_tb_member_TO_tb_thread
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_reply ADD CONSTRAINT FK_tb_member_TO_tb_reply
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_reply ADD CONSTRAINT FK_tb_thread_TO_tb_reply
FOREIGN KEY (thread_id) REFERENCES tb_thread (thread_id) ON DELETE CASCADE;

ALTER TABLE tb_forum_manager ADD CONSTRAINT FK_tb_member_TO_tb_forum_manager
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_member ADD CONSTRAINT FK_tb_role_TO_tb_member
FOREIGN KEY (role_id) REFERENCES tb_role (role_id) ON DELETE RESTRICT;

ALTER TABLE tb_thread ADD CONSTRAINT FK_tb_role_TO_tb_thread
FOREIGN KEY (role_id) REFERENCES tb_role (role_id) ON DELETE RESTRICT;

ALTER TABLE tb_like_thread ADD CONSTRAINT FK_tb_member_TO_tb_like_thread
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_like_thread ADD CONSTRAINT FK_tb_thread_TO_tb_like_thread
FOREIGN KEY (thread_id) REFERENCES tb_thread (thread_id) ON DELETE CASCADE;

ALTER TABLE tb_attachment_file ADD CONSTRAINT FK_tb_thread_TO_tb_attachment_file
FOREIGN KEY (thread_id) REFERENCES tb_thread (thread_id) ON DELETE CASCADE;

ALTER TABLE tb_hate_thread ADD CONSTRAINT FK_tb_member_TO_tb_hate_thread
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_hate_thread ADD CONSTRAINT FK_tb_thread_TO_tb_hate_thread
FOREIGN KEY (thread_id) REFERENCES tb_thread (thread_id) ON DELETE CASCADE;

ALTER TABLE tb_thread ADD CONSTRAINT FK_tb_forum_TO_tb_thread
FOREIGN KEY (forum_id) REFERENCES tb_forum (forum_id) ON DELETE CASCADE;

ALTER TABLE tb_forum_manager ADD CONSTRAINT FK_tb_forum_TO_tb_forum_manager
FOREIGN KEY (forum_id) REFERENCES tb_forum (forum_id) ON DELETE CASCADE;

ALTER TABLE tb_forum_subscribe ADD CONSTRAINT FK_tb_member_TO_tb_forum_subscribe
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_forum_subscribe ADD CONSTRAINT FK_tb_forum_TO_tb_forum_subscribe
FOREIGN KEY (forum_id) REFERENCES tb_forum (forum_id) ON DELETE CASCADE;

ALTER TABLE tb_thread_bookmark ADD CONSTRAINT FK_tb_member_TO_tb_thread_bookmark
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_thread_bookmark ADD CONSTRAINT FK_tb_thread_TO_tb_thread_bookmark
FOREIGN KEY (thread_id) REFERENCES tb_thread (thread_id) ON DELETE CASCADE;

ALTER TABLE tb_member ADD CONSTRAINT FK_tb_profile_image_TO_tb_member
FOREIGN KEY (profile_image_id) REFERENCES tb_profile_image (profile_image_id) ON DELETE SET NULL;

ALTER TABLE tb_reply ADD CONSTRAINT FK_tb_reply_TO_tb_reply
FOREIGN KEY (parent_id) REFERENCES tb_reply (reply_id) ON DELETE CASCADE;

ALTER TABLE tb_notification ADD CONSTRAINT FK_tb_member_TO_tb_notification
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_notification ADD CONSTRAINT FK_tb_member_TO_tb_notification1
FOREIGN KEY (sender_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_like_reply ADD CONSTRAINT FK_tb_reply_TO_tb_like_reply
FOREIGN KEY (reply_id) REFERENCES tb_reply (reply_id) ON DELETE CASCADE;

ALTER TABLE tb_hate_reply ADD CONSTRAINT FK_tb_reply_TO_tb_hate_reply
FOREIGN KEY (reply_id) REFERENCES tb_reply (reply_id) ON DELETE CASCADE;

ALTER TABLE tb_like_reply ADD CONSTRAINT FK_tb_member_TO_tb_like_reply
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_hate_reply ADD CONSTRAINT FK_tb_member_TO_tb_hate_reply
FOREIGN KEY (member_id) REFERENCES tb_member (member_id) ON DELETE CASCADE;

ALTER TABLE tb_thread_category ADD CONSTRAINT FK_tb_forum_TO_tb_thread_category
FOREIGN KEY (forum_id) REFERENCES tb_forum (forum_id) ON DELETE CASCADE;

ALTER TABLE tb_thread ADD CONSTRAINT FK_tb_thread_category_TO_tb_thread
FOREIGN KEY (thread_category_id) REFERENCES tb_thread_category (thread_category_id) ON DELETE SET NULL;


-- UNIQUE INDEX CONSTRAINT =================================================
CREATE UNIQUE INDEX UQ_tb_forum_manager_member_id_forum_id
ON tb_forum_manager (member_id ASC, forum_id ASC);

CREATE UNIQUE INDEX UQ_tb_forum_subscribe_member_id_forum_id
ON tb_forum_subscribe (member_id ASC, forum_id ASC);

CREATE UNIQUE INDEX UQ_tb_thread_bookmark_member_id_thread_id
ON tb_thread_bookmark (member_id ASC, thread_id ASC);

CREATE UNIQUE INDEX UQ_tb_hate_thread_member_id_thread_id
ON tb_hate_thread (member_id ASC, thread_id ASC);

CREATE UNIQUE INDEX UQ_tb_like_thread_member_id_thread_id
ON tb_like_thread (member_id ASC, thread_id ASC);

CREATE UNIQUE INDEX UQ_tb_like_reply_member_id_reply_id
ON tb_like_reply (member_id ASC, reply_id ASC);

CREATE UNIQUE INDEX UQ_tb_hate_reply_member_id_reply_id
ON tb_hate_reply (member_id ASC, reply_id ASC);

CREATE UNIQUE INDEX UQ_tb_thread_category_forum_id_category_name
ON tb_thread_category (forum_id ASC, category_name ASC);


-- CHECK CONSTRAINT =================================================
ALTER TABLE tb_forum_manager
ADD CONSTRAINT CHK_tb_forum_manager_fm_role
CHECK (fm_role IN ('manager','assistant'));

ALTER TABLE tb_notification
ADD CONSTRAINT CHK_tb_notification_noti_type
CHECK (noti_type IN ('like', 'mention', 'board', 'reply', 'notice')),
ADD CONSTRAINT CHK_tb_notification_reference_type
CHECK (reference_type IN ('board', 'reply'));


