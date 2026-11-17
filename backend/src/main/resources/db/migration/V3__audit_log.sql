-- ============================================================================
-- S2 · 操作审计日志
-- 说明：审计表只追加不修改；请求参数与前后值以 JSON 文本存储（便于直接展示，
--       且避免 jsonb 类型处理器带来的驱动兼容问题；后续如需按 JSON 检索可迁移为 jsonb）
-- ============================================================================

CREATE TABLE sys_audit_log (
    id             bigint       PRIMARY KEY,
    tenant_id      bigint       NOT NULL DEFAULT 1,
    trace_id       varchar(64),
    operator_id    varchar(64),
    operator_name  varchar(64),
    action         varchar(64)  NOT NULL,
    action_name    varchar(64),
    resource_type  varchar(32),
    resource_id    varchar(64),
    resource_name  varchar(128),
    request_method varchar(8),
    request_uri    varchar(256),
    request_params text,
    result         varchar(16)  NOT NULL DEFAULT 'SUCCESS',
    error_msg      varchar(1024),
    cost_ms        int,
    client_ip      varchar(64),
    user_agent     varchar(256),
    occurred_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_time     ON sys_audit_log (occurred_at DESC);
CREATE INDEX ix_audit_operator ON sys_audit_log (operator_id, occurred_at DESC);
CREATE INDEX ix_audit_action   ON sys_audit_log (action, occurred_at DESC);
CREATE INDEX ix_audit_resource ON sys_audit_log (resource_type, resource_id);

COMMENT ON TABLE  sys_audit_log IS '操作审计日志（只追加）';
COMMENT ON COLUMN sys_audit_log.action IS '动作编码，如 USER_CREATE / ROLE_ASSIGN_PERM';
COMMENT ON COLUMN sys_audit_log.action_name IS '动作中文名，便于审计员阅读';
COMMENT ON COLUMN sys_audit_log.request_params IS '请求参数 JSON（已脱敏：password 等替换为 ******）';
COMMENT ON COLUMN sys_audit_log.result IS 'SUCCESS / FAILED';
