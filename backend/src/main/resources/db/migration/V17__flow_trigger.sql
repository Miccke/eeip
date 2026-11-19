-- ============================================================================
-- S12 · 调度器：触发器运行时表
-- 流程发布时从 DSL 中扫描 SCHEDULE_CRON / MQ_SUBSCRIBE / EVENT_ON 节点，
-- 同步生成触发记录；调度线程扫描到期的 SCHEDULE 触发器并执行流程。
-- ============================================================================

CREATE TABLE t_flow_trigger (
    id             bigint       PRIMARY KEY,
    tenant_id      bigint       NOT NULL DEFAULT 1,
    flow_id        bigint       NOT NULL,
    version_id     bigint       NOT NULL,          -- 绑定到发布版本，未发布不生效
    flow_code      varchar(64)  NOT NULL,
    node_id        varchar(64)  NOT NULL,          -- DSL 中的触发器节点 id
    trigger_type   varchar(16)  NOT NULL,          -- SCHEDULE / MQ / EVENT / HTTP
    config_json    text,                           -- 节点 config 快照
    status         varchar(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / PAUSED / OFFLINE
    next_fire_at   timestamptz,                    -- 下次触发时间（SCHEDULE 专用）
    last_fire_at   timestamptz,
    last_status    varchar(16),                    -- SUCCESS / FAILED / SKIPPED
    last_instance  bigint,
    fire_count     int          NOT NULL DEFAULT 0,
    miss_count     int          NOT NULL DEFAULT 0,
    created_by     varchar(64),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    deleted        boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_trigger_node ON t_flow_trigger (tenant_id, flow_id, node_id) WHERE deleted = false;
CREATE INDEX ix_trigger_due ON t_flow_trigger (next_fire_at) WHERE status = 'ACTIVE' AND deleted = false;
CREATE INDEX ix_trigger_flow ON t_flow_trigger (flow_id);
COMMENT ON TABLE t_flow_trigger IS '流程触发器（由发布版本的 DSL 生成）';
COMMENT ON COLUMN t_flow_trigger.next_fire_at IS '下一次应触发时间；调度线程按此扫描';

-- 权限：触发计划查看/启停
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3127, 3102, 'BUTTON', 'asset:flow:trigger', '触发计划', NULL, NULL, NULL, 7);
UPDATE sys_permission SET name_tw = '觸發計畫', name_en = 'Triggers' WHERE id = 3127;
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(770001, 2002, 3127), (770011, 2003, 3127), (770021, 2101, 3127);
