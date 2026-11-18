-- ============================================================================
-- S11 · 执行引擎 v0：实例与节点执行记录
-- ============================================================================

CREATE TABLE t_flow_instance (
    id          bigint       PRIMARY KEY,
    tenant_id   bigint       NOT NULL DEFAULT 1,
    flow_id     bigint       NOT NULL,
    version_id  bigint,
    flow_code   varchar(64),
    trigger_type varchar(16) NOT NULL DEFAULT 'MANUAL',   -- MANUAL/HTTP/SCHEDULE/MQ/EVENT
    status      varchar(16)  NOT NULL DEFAULT 'RUNNING',  -- RUNNING/SUCCESS/FAILED
    input_json  text,
    output_json text,
    error       varchar(1024),
    cost_ms     int          NOT NULL DEFAULT 0,
    trace_id    varchar(64),
    started_at  timestamptz  NOT NULL DEFAULT now(),
    finished_at timestamptz,
    created_by  varchar(64),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE INDEX ix_instance_flow ON t_flow_instance (flow_id, started_at DESC) WHERE deleted = false;
CREATE INDEX ix_instance_status ON t_flow_instance (status, started_at DESC);

CREATE TABLE t_flow_instance_node (
    id          bigint      PRIMARY KEY,
    tenant_id   bigint      NOT NULL DEFAULT 1,
    instance_id bigint      NOT NULL,
    seq         int         NOT NULL DEFAULT 0,
    node_id     varchar(64) NOT NULL,
    node_type   varchar(32) NOT NULL,
    node_name   varchar(128),
    status      varchar(16) NOT NULL,   -- SUCCESS/FAILED/SKIPPED
    input_json  text,
    output_json text,
    error       varchar(1024),
    cost_ms     int         NOT NULL DEFAULT 0,
    started_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_instance_node ON t_flow_instance_node (instance_id, seq);

COMMENT ON TABLE t_flow_instance IS '流程执行实例';
COMMENT ON TABLE t_flow_instance_node IS '流程节点执行记录（调试瀑布图数据源）';

-- 接口开放：支持把请求交给流程执行
ALTER TABLE t_api_expose ADD COLUMN flow_code varchar(64);
COMMENT ON COLUMN t_api_expose.flow_code IS 'target_type=FLOW 时要执行的流程编码';

-- 权限：运行与实例查看
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3126, 3102, 'BUTTON', 'asset:flow:run',   '试运行', NULL, NULL, NULL, 6);
UPDATE sys_permission SET name_tw = '試運行', name_en = 'Run flow' WHERE id = 3126;
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(740001, 2002, 3126), (740011, 2003, 3126), (740021, 2101, 3126), (740031, 2102, 3126);
