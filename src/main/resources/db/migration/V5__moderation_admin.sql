CREATE TABLE tb_reports
(
  report_id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  reporter_user_id  BIGINT       NOT NULL,
  target_user_id    BIGINT      ,
  board_id          BIGINT      ,
  target_type       VARCHAR(24)  NOT NULL,
  target_id         BIGINT       NOT NULL,
  target_snapshot   TEXT        ,
  reason_code       VARCHAR(24)  NOT NULL,
  reason_detail     TEXT        ,
  status            VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
  processed_by      BIGINT      ,
  processed_at      TIMESTAMPTZ ,
  processed_note    TEXT        ,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  PRIMARY KEY (report_id)
);

COMMENT ON TABLE tb_reports IS '신고 접수/처리';

COMMENT ON COLUMN tb_reports.report_id IS '신고 번호';
COMMENT ON COLUMN tb_reports.reporter_user_id IS '신고자 회원번호';
COMMENT ON COLUMN tb_reports.target_user_id IS '신고대상 회원번호(작성자 등)';
COMMENT ON COLUMN tb_reports.board_id IS '관련 게시판 번호(없으면 NULL)';
COMMENT ON COLUMN tb_reports.target_type IS '신고대상 유형(ARTICLE/COMMENT/USER/BOARD)';
COMMENT ON COLUMN tb_reports.target_id IS '신고대상 식별자';
COMMENT ON COLUMN tb_reports.target_snapshot IS '신고 시점 대상 스냅샷(JSON 문자열 등)';
COMMENT ON COLUMN tb_reports.reason_code IS '신고 사유 코드';
COMMENT ON COLUMN tb_reports.reason_detail IS '신고 사유 상세';
COMMENT ON COLUMN tb_reports.status IS '처리 상태(PENDING/IN_REVIEW/RESOLVED/REJECTED)';
COMMENT ON COLUMN tb_reports.processed_by IS '처리자 회원번호';
COMMENT ON COLUMN tb_reports.processed_at IS '처리 일시';
COMMENT ON COLUMN tb_reports.processed_note IS '처리 메모';
COMMENT ON COLUMN tb_reports.created_at IS '생성일시';
COMMENT ON COLUMN tb_reports.updated_at IS '수정일시';



CREATE TABLE tb_sanctions
(
  sanction_id     BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id         BIGINT       NOT NULL,
  scope_type      VARCHAR(16)  NOT NULL,
  board_id        BIGINT      ,
  sanction_type   VARCHAR(24)  NOT NULL,
  reason          TEXT         NOT NULL,
  starts_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  ends_at         TIMESTAMPTZ ,
  report_id       BIGINT      ,
  created_by      BIGINT       NOT NULL,
  revoked_at      TIMESTAMPTZ ,
  revoked_by      BIGINT      ,
  revoked_reason  TEXT        ,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  PRIMARY KEY (sanction_id)
);

COMMENT ON TABLE tb_sanctions IS '제재 기록/기간';

COMMENT ON COLUMN tb_sanctions.sanction_id IS '제재 번호';
COMMENT ON COLUMN tb_sanctions.user_id IS '제재 대상 회원번호';
COMMENT ON COLUMN tb_sanctions.scope_type IS '제재 범위(GLOBAL/BOARD)';
COMMENT ON COLUMN tb_sanctions.board_id IS '게시판 범위 제재 대상(없으면 NULL)';
COMMENT ON COLUMN tb_sanctions.sanction_type IS '제재 유형(MUTE/SUSPEND/BAN 등)';
COMMENT ON COLUMN tb_sanctions.reason IS '제재 사유';
COMMENT ON COLUMN tb_sanctions.starts_at IS '제재 시작일시';
COMMENT ON COLUMN tb_sanctions.ends_at IS '제재 종료일시(NULL=무기한)';
COMMENT ON COLUMN tb_sanctions.report_id IS '연계 신고 번호';
COMMENT ON COLUMN tb_sanctions.created_by IS '제재 등록자 회원번호';
COMMENT ON COLUMN tb_sanctions.revoked_at IS '제재 해제 일시';
COMMENT ON COLUMN tb_sanctions.revoked_by IS '제재 해제자 회원번호';
COMMENT ON COLUMN tb_sanctions.revoked_reason IS '제재 해제 사유';
COMMENT ON COLUMN tb_sanctions.created_at IS '생성일시';
COMMENT ON COLUMN tb_sanctions.updated_at IS '수정일시';



CREATE TABLE tb_admin_audit_logs
(
  admin_log_id  BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
  actor_user_id BIGINT       NOT NULL,
  action_type   VARCHAR(48)  NOT NULL,
  target_type   VARCHAR(24)  NOT NULL,
  target_id     BIGINT      ,
  board_id      BIGINT      ,
  summary       VARCHAR(255) NOT NULL,
  detail_json   TEXT        ,
  ip_address    VARCHAR(64) ,
  user_agent    VARCHAR(255),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  PRIMARY KEY (admin_log_id)
);

COMMENT ON TABLE tb_admin_audit_logs IS '운영 로그/감사 기록';

COMMENT ON COLUMN tb_admin_audit_logs.admin_log_id IS '운영 로그 번호';
COMMENT ON COLUMN tb_admin_audit_logs.actor_user_id IS '행위자 회원번호';
COMMENT ON COLUMN tb_admin_audit_logs.action_type IS '행위 유형(예: REPORT_RESOLVE, SANCTION_CREATE)';
COMMENT ON COLUMN tb_admin_audit_logs.target_type IS '대상 유형(ARTICLE/COMMENT/USER/BOARD/REPORT/SANCTION 등)';
COMMENT ON COLUMN tb_admin_audit_logs.target_id IS '대상 식별자';
COMMENT ON COLUMN tb_admin_audit_logs.board_id IS '게시판 식별자(없으면 NULL)';
COMMENT ON COLUMN tb_admin_audit_logs.summary IS '요약 메시지';
COMMENT ON COLUMN tb_admin_audit_logs.detail_json IS '상세 데이터(JSON 문자열)';
COMMENT ON COLUMN tb_admin_audit_logs.ip_address IS '요청 IP';
COMMENT ON COLUMN tb_admin_audit_logs.user_agent IS '요청 User-Agent';
COMMENT ON COLUMN tb_admin_audit_logs.created_at IS '생성일시';



ALTER TABLE tb_reports
  ADD CONSTRAINT fk_tb_reports_reporter_user_id__tb_users
    FOREIGN KEY (reporter_user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_reports
  ADD CONSTRAINT fk_tb_reports_target_user_id__tb_users
    FOREIGN KEY (target_user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_reports
  ADD CONSTRAINT fk_tb_reports_board_id__tb_boards
    FOREIGN KEY (board_id)
    REFERENCES tb_boards (board_id);

ALTER TABLE tb_reports
  ADD CONSTRAINT fk_tb_reports_processed_by__tb_users
    FOREIGN KEY (processed_by)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_sanctions
  ADD CONSTRAINT fk_tb_sanctions_user_id__tb_users
    FOREIGN KEY (user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_sanctions
  ADD CONSTRAINT fk_tb_sanctions_board_id__tb_boards
    FOREIGN KEY (board_id)
    REFERENCES tb_boards (board_id);

ALTER TABLE tb_sanctions
  ADD CONSTRAINT fk_tb_sanctions_report_id__tb_reports
    FOREIGN KEY (report_id)
    REFERENCES tb_reports (report_id);

ALTER TABLE tb_sanctions
  ADD CONSTRAINT fk_tb_sanctions_created_by__tb_users
    FOREIGN KEY (created_by)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_sanctions
  ADD CONSTRAINT fk_tb_sanctions_revoked_by__tb_users
    FOREIGN KEY (revoked_by)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_admin_audit_logs
  ADD CONSTRAINT fk_tb_admin_audit_logs_actor_user_id__tb_users
    FOREIGN KEY (actor_user_id)
    REFERENCES tb_users (user_id);

ALTER TABLE tb_admin_audit_logs
  ADD CONSTRAINT fk_tb_admin_audit_logs_board_id__tb_boards
    FOREIGN KEY (board_id)
    REFERENCES tb_boards (board_id);

CREATE INDEX IF NOT EXISTS ix_tb_reports_status_created_at
  ON tb_reports (status, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_reports_board_id_status_created_at
  ON tb_reports (board_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_reports_target_type_target_id
  ON tb_reports (target_type, target_id);

CREATE INDEX IF NOT EXISTS ix_tb_reports_reporter_user_id_created_at
  ON tb_reports (reporter_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_sanctions_user_id_ends_at
  ON tb_sanctions (user_id, ends_at);

CREATE INDEX IF NOT EXISTS ix_tb_sanctions_scope_type_board_id
  ON tb_sanctions (scope_type, board_id);

CREATE INDEX IF NOT EXISTS ix_tb_sanctions_report_id
  ON tb_sanctions (report_id);

CREATE INDEX IF NOT EXISTS ix_tb_admin_audit_logs_actor_user_id_created_at
  ON tb_admin_audit_logs (actor_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_admin_audit_logs_action_type_created_at
  ON tb_admin_audit_logs (action_type, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_admin_audit_logs_target_type_target_id
  ON tb_admin_audit_logs (target_type, target_id);
