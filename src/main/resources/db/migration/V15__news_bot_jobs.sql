CREATE TABLE tb_news_collection_jobs
(
  news_job_id                BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  job_name                   VARCHAR(120)  NOT NULL,
  source_type                VARCHAR(32)   NOT NULL,
  source_config_json         TEXT          NOT NULL,
  target_board_slug          VARCHAR(80)   NOT NULL,
  target_board_name          VARCHAR(255),
  target_category_name       VARCHAR(48),
  author_user_id             BIGINT        NOT NULL,
  created_by_user_id         BIGINT        NOT NULL,
  updated_by_user_id         BIGINT        NOT NULL,
  is_enabled                 BOOLEAN       NOT NULL DEFAULT true,
  collect_interval_minutes   INTEGER       NOT NULL,
  fetch_limit                INTEGER       NOT NULL,
  is_auto_create_board       BOOLEAN       NOT NULL DEFAULT false,
  is_auto_create_category    BOOLEAN       NOT NULL DEFAULT true,
  timezone                   VARCHAR(64)   NOT NULL DEFAULT 'Asia/Seoul',
  last_started_at            TIMESTAMPTZ,
  last_finished_at           TIMESTAMPTZ,
  last_success_at            TIMESTAMPTZ,
  next_run_at                TIMESTAMPTZ,
  last_status                VARCHAR(24)   NOT NULL DEFAULT 'IDLE',
  last_error_message         TEXT,
  created_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
  PRIMARY KEY (news_job_id),
  CONSTRAINT ck_tb_news_collection_jobs_collect_interval_minutes
    CHECK (collect_interval_minutes BETWEEN 5 AND 10080),
  CONSTRAINT ck_tb_news_collection_jobs_fetch_limit
    CHECK (fetch_limit BETWEEN 1 AND 100)
);

COMMENT ON TABLE tb_news_collection_jobs IS '뉴스봇 수집 잡 정의 및 운영 제어 정보';

COMMENT ON COLUMN tb_news_collection_jobs.news_job_id IS '뉴스 수집 잡 PK';
COMMENT ON COLUMN tb_news_collection_jobs.job_name IS '백오피스에서 식별하는 잡 이름';
COMMENT ON COLUMN tb_news_collection_jobs.source_type IS '외부 소스 유형';
COMMENT ON COLUMN tb_news_collection_jobs.source_config_json IS '외부 소스별 설정 JSON 문자열';
COMMENT ON COLUMN tb_news_collection_jobs.target_board_slug IS '수집 결과를 적재할 대상 게시판 slug';
COMMENT ON COLUMN tb_news_collection_jobs.target_board_name IS '게시판 자동 생성 시 사용할 게시판 이름';
COMMENT ON COLUMN tb_news_collection_jobs.target_category_name IS '기본 대상 카테고리 이름';
COMMENT ON COLUMN tb_news_collection_jobs.author_user_id IS '실제 뉴스 게시글 작성자 user_id';
COMMENT ON COLUMN tb_news_collection_jobs.created_by_user_id IS '잡 생성 관리자 user_id';
COMMENT ON COLUMN tb_news_collection_jobs.updated_by_user_id IS '잡 최종 수정 관리자 user_id';
COMMENT ON COLUMN tb_news_collection_jobs.is_enabled IS '잡 활성화 여부';
COMMENT ON COLUMN tb_news_collection_jobs.collect_interval_minutes IS '수집 주기(분)';
COMMENT ON COLUMN tb_news_collection_jobs.fetch_limit IS '1회 수집 최대 항목 수';
COMMENT ON COLUMN tb_news_collection_jobs.is_auto_create_board IS '대상 게시판 자동 생성 허용 여부';
COMMENT ON COLUMN tb_news_collection_jobs.is_auto_create_category IS '대상 카테고리 자동 생성 허용 여부';
COMMENT ON COLUMN tb_news_collection_jobs.timezone IS '잡 실행 기준 시간대';
COMMENT ON COLUMN tb_news_collection_jobs.last_started_at IS '마지막 실행 시작 시각';
COMMENT ON COLUMN tb_news_collection_jobs.last_finished_at IS '마지막 실행 종료 시각';
COMMENT ON COLUMN tb_news_collection_jobs.last_success_at IS '마지막 성공 시각';
COMMENT ON COLUMN tb_news_collection_jobs.next_run_at IS '다음 실행 예정 시각';
COMMENT ON COLUMN tb_news_collection_jobs.last_status IS '마지막 실행 상태';
COMMENT ON COLUMN tb_news_collection_jobs.last_error_message IS '마지막 실패 메시지';

ALTER TABLE tb_news_collection_jobs
  ADD CONSTRAINT uq_tb_news_collection_jobs_job_name
    UNIQUE (job_name);

ALTER TABLE tb_news_collection_jobs
  ADD CONSTRAINT fk_tb_news_collection_jobs_author_user_id__tb_users
    FOREIGN KEY (author_user_id) REFERENCES tb_users (user_id);

ALTER TABLE tb_news_collection_jobs
  ADD CONSTRAINT fk_tb_news_collection_jobs_created_by_user_id__tb_users
    FOREIGN KEY (created_by_user_id) REFERENCES tb_users (user_id);

ALTER TABLE tb_news_collection_jobs
  ADD CONSTRAINT fk_tb_news_collection_jobs_updated_by_user_id__tb_users
    FOREIGN KEY (updated_by_user_id) REFERENCES tb_users (user_id);

CREATE INDEX IF NOT EXISTS ix_tb_news_collection_jobs_enabled_next_run_at
  ON tb_news_collection_jobs (is_enabled, next_run_at);

CREATE INDEX IF NOT EXISTS ix_tb_news_collection_jobs_source_type
  ON tb_news_collection_jobs (source_type);

CREATE INDEX IF NOT EXISTS ix_tb_news_collection_jobs_author_user_id
  ON tb_news_collection_jobs (author_user_id);

CREATE INDEX IF NOT EXISTS ix_tb_news_collection_jobs_created_by_user_id
  ON tb_news_collection_jobs (created_by_user_id);

CREATE TABLE tb_news_collected_items
(
  news_collected_item_id BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  news_job_id            BIGINT        NOT NULL,
  external_item_key      VARCHAR(255)  NOT NULL,
  external_url           TEXT          NOT NULL,
  title                  VARCHAR(255)  NOT NULL,
  payload_hash           VARCHAR(64)   NOT NULL,
  published_at           TIMESTAMPTZ,
  source_updated_at      TIMESTAMPTZ,
  article_id             BIGINT,
  last_sync_status       VARCHAR(24)   NOT NULL,
  last_error_message     TEXT,
  first_collected_at     TIMESTAMPTZ   NOT NULL,
  last_collected_at      TIMESTAMPTZ   NOT NULL,
  last_synced_at         TIMESTAMPTZ,
  created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
  PRIMARY KEY (news_collected_item_id)
);

COMMENT ON TABLE tb_news_collected_items IS '외부 뉴스 항목과 내부 게시글 연결 및 중복 방지 정보';

COMMENT ON COLUMN tb_news_collected_items.news_collected_item_id IS '수집 항목 PK';
COMMENT ON COLUMN tb_news_collected_items.news_job_id IS '수집 잡 FK';
COMMENT ON COLUMN tb_news_collected_items.external_item_key IS '외부 항목 고유 키';
COMMENT ON COLUMN tb_news_collected_items.external_url IS '외부 원문 URL';
COMMENT ON COLUMN tb_news_collected_items.title IS '수집 당시 제목';
COMMENT ON COLUMN tb_news_collected_items.payload_hash IS '수집 payload 해시';
COMMENT ON COLUMN tb_news_collected_items.published_at IS '원문 발행 시각';
COMMENT ON COLUMN tb_news_collected_items.source_updated_at IS '원문 수정 시각';
COMMENT ON COLUMN tb_news_collected_items.article_id IS '내부 게시글 FK';
COMMENT ON COLUMN tb_news_collected_items.last_sync_status IS '마지막 동기화 상태';
COMMENT ON COLUMN tb_news_collected_items.last_error_message IS '마지막 실패 메시지';
COMMENT ON COLUMN tb_news_collected_items.first_collected_at IS '최초 수집 시각';
COMMENT ON COLUMN tb_news_collected_items.last_collected_at IS '마지막 수집 시각';
COMMENT ON COLUMN tb_news_collected_items.last_synced_at IS '마지막 내부 반영 시각';

ALTER TABLE tb_news_collected_items
  ADD CONSTRAINT uq_tb_news_collected_items_news_job_id_external_item_key
    UNIQUE (news_job_id, external_item_key);

ALTER TABLE tb_news_collected_items
  ADD CONSTRAINT fk_tb_news_collected_items_news_job_id__tb_news_collection_jobs
    FOREIGN KEY (news_job_id) REFERENCES tb_news_collection_jobs (news_job_id);

ALTER TABLE tb_news_collected_items
  ADD CONSTRAINT fk_tb_news_collected_items_article_id__tb_articles
    FOREIGN KEY (article_id) REFERENCES tb_articles (article_id);

CREATE INDEX IF NOT EXISTS ix_tb_news_collected_items_article_id
  ON tb_news_collected_items (article_id);

CREATE INDEX IF NOT EXISTS ix_tb_news_collected_items_news_job_id_last_sync_status
  ON tb_news_collected_items (news_job_id, last_sync_status);

CREATE INDEX IF NOT EXISTS ix_tb_news_collected_items_news_job_id_published_at
  ON tb_news_collected_items (news_job_id, published_at DESC);

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
  role.role_id,
  'news_bot',
  'news-bot@dummy.du',
  '$argon2id$v=19$m=16384,t=2,p=1$uXob/dB7g0RnR5HNjR1daw$gasEEOjrRqtZXCkUKe/6Ndpe3hCSLyUl5QG901b4YL4',
  '뉴스봇 시스템',
  '뉴스봇',
  'news_bot',
  0,
  false,
  false,
  true
FROM tb_role role
WHERE role.role_name = 'WRITER'
  AND NOT EXISTS (
    SELECT 1
    FROM tb_users existing
    WHERE existing.login_id = 'news_bot'
       OR existing.email = 'news-bot@dummy.du'
       OR existing.handle = 'news_bot'
  );
