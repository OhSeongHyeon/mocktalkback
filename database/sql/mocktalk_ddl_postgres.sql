-- PostgreSQL DDL for mocktalkback (entity-based)

CREATE TABLE tb_role (
    role_id     BIGSERIAL PRIMARY KEY,
    role_name   VARCHAR(24) NOT NULL,
    auth_bit    INT         NOT NULL,
    description VARCHAR(36),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE tb_users (
    user_id           BIGSERIAL PRIMARY KEY,
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
    deleted_at        TIMESTAMPTZ
);

CREATE TABLE tb_user_oauth_links (
    user_oauth_link_id BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    provider           VARCHAR(16)  NOT NULL,
    provider_id        VARCHAR(128) NOT NULL,
    email              VARCHAR(128),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tb_file_classes (
    file_class_id   BIGSERIAL PRIMARY KEY,
    file_class_code VARCHAR(32) NOT NULL,
    file_class_name VARCHAR(64) NOT NULL,
    description     VARCHAR(255),
    media_kind      VARCHAR(16) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE tb_files (
    file_id       BIGSERIAL PRIMARY KEY,
    file_class_id BIGINT        NOT NULL,
    file_name     VARCHAR(255)  NOT NULL,
    storage_key   VARCHAR(1024) NOT NULL,
    file_size     BIGINT        NOT NULL,
    mime_type     VARCHAR(128)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE tb_user_files (
    user_file_id BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    file_id      BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE tb_role
    ADD CONSTRAINT uq_role_role_name UNIQUE (role_name);

ALTER TABLE tb_users
    ADD CONSTRAINT uq_tb_users_login_id UNIQUE (login_id);

ALTER TABLE tb_users
    ADD CONSTRAINT uq_tb_users_email UNIQUE (email);

ALTER TABLE tb_users
    ADD CONSTRAINT uq_tb_users_handle UNIQUE (handle);

ALTER TABLE tb_user_oauth_links
    ADD CONSTRAINT uq_tb_user_oauth_links_provider_user UNIQUE (provider, provider_id);

ALTER TABLE tb_user_oauth_links
    ADD CONSTRAINT uq_tb_user_oauth_links_user_provider UNIQUE (user_id, provider);

ALTER TABLE tb_users
    ADD CONSTRAINT fk_tb_users_tb_role
    FOREIGN KEY (role_id) REFERENCES tb_role (role_id);

ALTER TABLE tb_user_oauth_links
    ADD CONSTRAINT fk_tb_user_oauth_links_tb_users
    FOREIGN KEY (user_id) REFERENCES tb_users (user_id);

ALTER TABLE tb_files
    ADD CONSTRAINT fk_tb_files_tb_file_classes
    FOREIGN KEY (file_class_id) REFERENCES tb_file_classes (file_class_id);

ALTER TABLE tb_user_files
    ADD CONSTRAINT fk_tb_user_files_tb_users
    FOREIGN KEY (user_id) REFERENCES tb_users (user_id);

ALTER TABLE tb_user_files
    ADD CONSTRAINT fk_tb_user_files_tb_files
    FOREIGN KEY (file_id) REFERENCES tb_files (file_id);

CREATE INDEX ix_tb_users_role_id ON tb_users (role_id);
CREATE INDEX ix_tb_user_files_user_id ON tb_user_files (user_id);
CREATE INDEX ix_tb_user_files_file_id ON tb_user_files (file_id);


/* =========================
 * mocktalkback - COMMENTS
 * ========================= */

/* ---------- tb_role ---------- */
COMMENT ON TABLE tb_role IS '역할(권한) 마스터 테이블';

COMMENT ON COLUMN tb_role.role_id     IS '역할 PK';
COMMENT ON COLUMN tb_role.role_name   IS '역할명(유니크)';
COMMENT ON COLUMN tb_role.auth_bit    IS '권한 비트마스크 값(INT)';
COMMENT ON COLUMN tb_role.description IS '역할 설명';
COMMENT ON COLUMN tb_role.created_at  IS '생성 시각';
COMMENT ON COLUMN tb_role.updated_at  IS '수정 시각';
COMMENT ON COLUMN tb_role.deleted_at  IS '소프트 삭제 시각(NULL이면 활성)';

COMMENT ON CONSTRAINT uq_role_role_name ON tb_role IS '역할명 유니크 제약';

/* ---------- tb_users ---------- */
COMMENT ON TABLE tb_users IS '사용자(계정) 테이블';

COMMENT ON COLUMN tb_users.user_id           IS '사용자 PK';
COMMENT ON COLUMN tb_users.role_id           IS '역할 FK(tb_role.role_id)';
COMMENT ON COLUMN tb_users.login_id          IS '로그인 ID(로그인에 사용되는 계정 식별자)';
COMMENT ON COLUMN tb_users.email             IS '로그인 이메일(유니크)';
COMMENT ON COLUMN tb_users.pw_hash           IS '비밀번호 해시(평문 금지)';
COMMENT ON COLUMN tb_users.user_name         IS '실명/이름(서비스 정책에 따라 의미 정의)';
COMMENT ON COLUMN tb_users.display_name      IS '노출 닉네임(표시용)';
COMMENT ON COLUMN tb_users.handle            IS '고유 핸들/아이디(유니크, URL/멘션용)';
COMMENT ON COLUMN tb_users.user_point        IS '포인트/점수';
COMMENT ON COLUMN tb_users.is_email_verified IS '이메일 인증 여부(true=이메일 인증자)';
COMMENT ON COLUMN tb_users.is_enabled        IS '계정 활성 여부(true=로그인/사용 가능)';
COMMENT ON COLUMN tb_users.is_locked         IS '계정 잠금 여부(true=로그인 차단/제한)';
COMMENT ON COLUMN tb_users.created_at        IS '생성 시각';
COMMENT ON COLUMN tb_users.updated_at        IS '수정 시각';
COMMENT ON COLUMN tb_users.deleted_at        IS '소프트 삭제 시각(NULL이면 활성)';

COMMENT ON CONSTRAINT uq_tb_users_login_id ON tb_users IS '로그인 ID 유니크 제약';
COMMENT ON CONSTRAINT uq_tb_users_email    ON tb_users IS '이메일 유니크 제약';
COMMENT ON CONSTRAINT uq_tb_users_handle   ON tb_users IS '핸들 유니크 제약';
COMMENT ON CONSTRAINT fk_tb_users_tb_role  ON tb_users IS '역할 참조 FK(tb_role)';

COMMENT ON INDEX ix_tb_users_role_id IS '역할별 사용자 조회 최적화(role_id)';

/* ---------- tb_user_oauth_links ---------- */
COMMENT ON TABLE tb_user_oauth_links IS '사용자 OAuth 연동 테이블';

COMMENT ON COLUMN tb_user_oauth_links.user_oauth_link_id IS 'OAuth 연동 PK';
COMMENT ON COLUMN tb_user_oauth_links.user_id            IS '사용자 FK(tb_users.user_id)';
COMMENT ON COLUMN tb_user_oauth_links.provider           IS 'OAuth 제공자(google/github 등)';
COMMENT ON COLUMN tb_user_oauth_links.provider_id        IS 'OAuth 제공자 사용자 ID';
COMMENT ON COLUMN tb_user_oauth_links.email              IS '제공자 이메일(표시/로그 용)';
COMMENT ON COLUMN tb_user_oauth_links.created_at         IS '생성 시각';
COMMENT ON COLUMN tb_user_oauth_links.updated_at         IS '수정 시각';

COMMENT ON CONSTRAINT uq_tb_user_oauth_links_provider_user ON tb_user_oauth_links IS '제공자+제공자ID 유니크 제약';
COMMENT ON CONSTRAINT uq_tb_user_oauth_links_user_provider ON tb_user_oauth_links IS '사용자+제공자 유니크 제약';
COMMENT ON CONSTRAINT fk_tb_user_oauth_links_tb_users      ON tb_user_oauth_links IS '사용자 참조 FK(tb_users)';

/* ---------- tb_file_classes ---------- */
COMMENT ON TABLE tb_file_classes IS '파일 분류(카테고리/정책) 마스터 테이블';

COMMENT ON COLUMN tb_file_classes.file_class_id   IS '파일 분류 PK';
COMMENT ON COLUMN tb_file_classes.file_class_code IS '파일 분류 코드(시스템 키)';
COMMENT ON COLUMN tb_file_classes.file_class_name IS '파일 분류명(표시용)';
COMMENT ON COLUMN tb_file_classes.description     IS '설명';
COMMENT ON COLUMN tb_file_classes.media_kind      IS '미디어 종류(예: image/video/audio/document 등)';
COMMENT ON COLUMN tb_file_classes.created_at      IS '생성 시각';
COMMENT ON COLUMN tb_file_classes.updated_at      IS '수정 시각';
COMMENT ON COLUMN tb_file_classes.deleted_at      IS '소프트 삭제 시각(NULL이면 활성)';

/* ---------- tb_files ---------- */
COMMENT ON TABLE tb_files IS '파일 메타데이터(저장소 키 기반) 테이블';

COMMENT ON COLUMN tb_files.file_id       IS '파일 PK';
COMMENT ON COLUMN tb_files.file_class_id IS '파일 분류 FK(tb_file_classes.file_class_id)';
COMMENT ON COLUMN tb_files.file_name     IS '원본 파일명(표시용)';
COMMENT ON COLUMN tb_files.storage_key   IS '실제 저장소 키/경로(S3 key 등)';
COMMENT ON COLUMN tb_files.file_size     IS '파일 크기(bytes)';
COMMENT ON COLUMN tb_files.mime_type     IS 'MIME 타입(예: image/png)';
COMMENT ON COLUMN tb_files.created_at    IS '생성 시각';
COMMENT ON COLUMN tb_files.updated_at    IS '수정 시각';
COMMENT ON COLUMN tb_files.deleted_at    IS '소프트 삭제 시각(NULL이면 활성)';

COMMENT ON CONSTRAINT fk_tb_files_tb_file_classes ON tb_files IS '파일 분류 참조 FK(tb_file_classes)';

/* ---------- tb_user_files ---------- */
COMMENT ON TABLE tb_user_files IS '사용자-파일 매핑(소유/첨부 관계) 테이블';

COMMENT ON COLUMN tb_user_files.user_file_id IS '매핑 PK';
COMMENT ON COLUMN tb_user_files.user_id      IS '사용자 FK(tb_users.user_id)';
COMMENT ON COLUMN tb_user_files.file_id      IS '파일 FK(tb_files.file_id)';
COMMENT ON COLUMN tb_user_files.created_at   IS '생성 시각';
COMMENT ON COLUMN tb_user_files.updated_at   IS '수정 시각';

COMMENT ON CONSTRAINT fk_tb_user_files_tb_users ON tb_user_files IS '사용자 참조 FK(tb_users)';
COMMENT ON CONSTRAINT fk_tb_user_files_tb_files ON tb_user_files IS '파일 참조 FK(tb_files)';

COMMENT ON INDEX ix_tb_user_files_user_id IS '사용자별 파일 목록 조회 최적화(user_id)';
COMMENT ON INDEX ix_tb_user_files_file_id IS '파일 기준 역참조 조회 최적화(file_id)';
