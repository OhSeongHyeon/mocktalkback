CREATE TABLE tb_file_variants
(
  variant_id    BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  file_id       BIGINT        NOT NULL,
  variant_code  VARCHAR(32)   NOT NULL,
  storage_key   VARCHAR(1024) NOT NULL,
  file_size     BIGINT        NOT NULL,
  mime_type     VARCHAR(128)  NOT NULL,
  width         INTEGER       NOT NULL,
  height        INTEGER       NOT NULL,
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ  ,
  PRIMARY KEY (variant_id)
);

COMMENT ON TABLE tb_file_variants IS '이미지 변환본(최적화본) 메타데이터 테이블';

COMMENT ON COLUMN tb_file_variants.variant_id IS '변환파일번호';
COMMENT ON COLUMN tb_file_variants.file_id IS '원본 파일번호';
COMMENT ON COLUMN tb_file_variants.variant_code IS '변환본 코드(예: THUMB/MEDIUM/LARGE)';
COMMENT ON COLUMN tb_file_variants.storage_key IS '실제 저장소 키/경로';
COMMENT ON COLUMN tb_file_variants.file_size IS '파일 크기 byte';
COMMENT ON COLUMN tb_file_variants.mime_type IS 'MIME 타입(예: image/webp)';
COMMENT ON COLUMN tb_file_variants.width IS '가로 픽셀';
COMMENT ON COLUMN tb_file_variants.height IS '세로 픽셀';
COMMENT ON COLUMN tb_file_variants.created_at IS '생성일자';
COMMENT ON COLUMN tb_file_variants.updated_at IS '수정일자';
COMMENT ON COLUMN tb_file_variants.deleted_at IS '삭제시각';

ALTER TABLE tb_file_variants
  ADD CONSTRAINT fk_tb_file_variants_file_id__tb_files
    FOREIGN KEY (file_id)
    REFERENCES tb_files (file_id);

ALTER TABLE tb_file_variants
  ADD CONSTRAINT uq_tb_file_variants_storage_key UNIQUE (storage_key);

ALTER TABLE tb_file_variants
  ADD CONSTRAINT uq_tb_file_variants_file_id_variant_code UNIQUE (file_id, variant_code);

ALTER TABLE tb_files
  ADD COLUMN metadata_preserved BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN tb_files.metadata_preserved IS '원본 메타데이터(EXIF 등) 보존 여부';

ALTER TABLE tb_files
  ADD COLUMN temp_expires_at TIMESTAMPTZ;

COMMENT ON COLUMN tb_files.temp_expires_at IS '임시 업로드 만료 시각';

CREATE INDEX ix_tb_files_temp_expires_at
  ON tb_files (temp_expires_at)
  WHERE temp_expires_at IS NOT NULL;

