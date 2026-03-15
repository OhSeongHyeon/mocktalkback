CREATE TABLE tb_market_snapshots
(
  market_snapshot_id BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  instrument_code    VARCHAR(32)   NOT NULL,
  market_group       VARCHAR(16)   NOT NULL,
  provider_name      VARCHAR(32)   NOT NULL,
  base_currency      VARCHAR(16)   NOT NULL,
  quote_currency     VARCHAR(16)   NOT NULL,
  price_value        NUMERIC(20,8) NOT NULL,
  change_value       NUMERIC(20,8),
  change_rate        NUMERIC(12,6),
  observed_at        TIMESTAMPTZ   NOT NULL,
  created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  PRIMARY KEY (market_snapshot_id)
);

COMMENT ON TABLE tb_market_snapshots IS '환율/금값 시세 스냅샷';

COMMENT ON COLUMN tb_market_snapshots.market_snapshot_id IS '시세 스냅샷 PK';
COMMENT ON COLUMN tb_market_snapshots.instrument_code IS '종목 코드';
COMMENT ON COLUMN tb_market_snapshots.market_group IS '시세 그룹';
COMMENT ON COLUMN tb_market_snapshots.provider_name IS '데이터 공급자명';
COMMENT ON COLUMN tb_market_snapshots.base_currency IS '기준 통화 또는 자산 코드';
COMMENT ON COLUMN tb_market_snapshots.quote_currency IS '비교 통화';
COMMENT ON COLUMN tb_market_snapshots.price_value IS '시세 값';
COMMENT ON COLUMN tb_market_snapshots.change_value IS '직전 저장값 대비 변화량';
COMMENT ON COLUMN tb_market_snapshots.change_rate IS '직전 저장값 대비 변화율';
COMMENT ON COLUMN tb_market_snapshots.observed_at IS '외부 공급자 기준 시각';
COMMENT ON COLUMN tb_market_snapshots.created_at IS '내부 저장 시각';

ALTER TABLE tb_market_snapshots
  ADD CONSTRAINT uq_tb_market_snapshots_instrument_code_observed_at
    UNIQUE (instrument_code, observed_at);

ALTER TABLE tb_market_snapshots
  ADD CONSTRAINT ck_tb_market_snapshots_market_group
    CHECK (market_group IN ('FX', 'METAL'));

ALTER TABLE tb_market_snapshots
  ADD CONSTRAINT ck_tb_market_snapshots_price_value
    CHECK (price_value >= 0);

CREATE INDEX IF NOT EXISTS ix_tb_market_snapshots_instrument_code_observed_at
  ON tb_market_snapshots (instrument_code, observed_at DESC);

CREATE INDEX IF NOT EXISTS ix_tb_market_snapshots_market_group_observed_at
  ON tb_market_snapshots (market_group, observed_at DESC);
