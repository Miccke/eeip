-- ============================================================================
-- S14 · 重试与死信
-- 节点级重试（指数退避）+ 失败实例落死信 + 死信重投
-- ============================================================================

-- 节点轨迹补充：重试次数
ALTER TABLE t_flow_instance_node ADD COLUMN attempts int NOT NULL DEFAULT 1;
COMMENT ON COLUMN t_flow_instance_node.attempts IS '本节点实际执行次数（含重试）';

-- 死信表
CREATE TABLE t_flow_dead_letter (
    id              bigint       PRIMARY KEY,
    tenant_id       bigint       NOT NULL DEFAULT 1,
    instance_id     bigint       NOT NULL,
    flow_id         bigint       NOT NULL,
    flow_code       varchar(64),
    flow_name       varchar(128),
    version_id      bigint,
    node_id         varchar(64),
    node_name       varchar(128),
    node_type       varchar(32),
    error           varchar(1024),
    input_json      text,
    attempts        int          NOT NULL DEFAULT 1,
    status          varchar(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING/RETRIED/DISCARDED
    retried_instance bigint,
    retry_count     int          NOT NULL DEFAULT 0,
    handled_by      varchar(64),
    handled_at      timestamptz,
    remark          varchar(256),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    deleted         boolean      NOT NULL DEFAULT false
);
CREATE INDEX ix_dlq_status ON t_flow_dead_letter (status, created_at DESC) WHERE deleted = false;
CREATE INDEX ix_dlq_flow ON t_flow_dead_letter (flow_id);
CREATE UNIQUE INDEX ux_dlq_instance ON t_flow_dead_letter (instance_id) WHERE deleted = false;
COMMENT ON TABLE t_flow_dead_letter IS '死信：执行失败的实例与原始入参，可重投或丢弃';

-- 节点规格：给 HTTP_INVOKE 增加重试配置（其余节点可后续按需加）
-- 说明：retry 是引擎级通用配置，manifest 只做 UI 呈现，缺失也不影响执行
