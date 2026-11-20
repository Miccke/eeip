-- ============================================================================
-- S15 · 调用计量
-- 每个接口按分钟聚合：调用次数、失败次数、总耗时，用于资产页的"30 日调用/错误率"
-- ============================================================================

CREATE TABLE t_api_metric (
    id          bigint      PRIMARY KEY,
    tenant_id   bigint      NOT NULL DEFAULT 1,
    expose_id   bigint      NOT NULL,            -- 对外发布记录
    api_id      bigint,                          -- 台账接口（可能为空：老数据）
    bucket      timestamptz NOT NULL,            -- 分钟桶（秒归零）
    calls       bigint      NOT NULL DEFAULT 0,
    errors      bigint      NOT NULL DEFAULT 0,
    cost_ms_sum bigint      NOT NULL DEFAULT 0,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_metric_bucket ON t_api_metric (expose_id, bucket);
CREATE INDEX ix_metric_bucket ON t_api_metric (bucket DESC);
CREATE INDEX ix_metric_api ON t_api_metric (api_id, bucket DESC);
COMMENT ON TABLE t_api_metric IS '接口调用计量（分钟粒度）';

-- 接口台账列表读计量用
ALTER TABLE t_api_expose ADD COLUMN metric_enabled smallint NOT NULL DEFAULT 1;
