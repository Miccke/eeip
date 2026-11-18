-- ============================================================================
-- S7 · 流程定义与版本（DSL 存储）
-- 对齐详细设计 03 章 t_flow / t_flow_version，当前落 public schema
-- ============================================================================

CREATE TABLE t_flow (
    id                 bigint       PRIMARY KEY,
    tenant_id          bigint       NOT NULL DEFAULT 1,
    flow_code          varchar(64)  NOT NULL,
    flow_name          varchar(128) NOT NULL,
    trigger_type       varchar(16)  NOT NULL DEFAULT 'HTTP',   -- HTTP/MQ/SCHEDULE/EVENT
    status             varchar(16)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT/PUBLISHED/OFFLINE
    current_version_id bigint,
    description        varchar(1024),
    created_by         varchar(64),
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_by         varchar(64),
    updated_at         timestamptz  NOT NULL DEFAULT now(),
    deleted            boolean      NOT NULL DEFAULT false,
    version            int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_flow_code ON t_flow (tenant_id, flow_code) WHERE deleted = false;
CREATE INDEX ix_flow_status ON t_flow (status) WHERE deleted = false;

CREATE TABLE t_flow_version (
    id            bigint      PRIMARY KEY,
    tenant_id     bigint      NOT NULL DEFAULT 1,
    flow_id       bigint      NOT NULL,
    version       varchar(16) NOT NULL,               -- 1.0.0 / 1.1.0
    dsl_json      jsonb       NOT NULL,               -- 完整流程定义（06 章 §2）
    dsl_hash      varchar(64) NOT NULL,               -- SHA-256，变更检测与缓存 key
    node_count    int         NOT NULL DEFAULT 0,
    status        varchar(16) NOT NULL DEFAULT 'DRAFT',
                  -- DRAFT/PUBLISHED/DEPRECATED
    change_log    varchar(2048),
    published_at  timestamptz,
    published_by  varchar(64),
    created_by    varchar(64),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deleted       boolean     NOT NULL DEFAULT false,
    version_lock  int         NOT NULL DEFAULT 0
);
CREATE INDEX ix_flowv_flow ON t_flow_version (flow_id) WHERE deleted = false;
CREATE INDEX ix_flowv_status ON t_flow_version (flow_id, status);
COMMENT ON TABLE  t_flow IS '集成流程定义（元数据）';
COMMENT ON TABLE  t_flow_version IS '流程版本（DSL 全文 + 摘要哈希）';
COMMENT ON COLUMN t_flow_version.dsl_hash IS 'SHA-256(规范化 DSL)，用于变更检测与运行时缓存 key';

-- 权限点：编辑 / 删除 / 校验（新建与发布已存在）
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3123, 3102, 'BUTTON', 'asset:flow:update',   '编辑流程', NULL, NULL, NULL, 3),
(3124, 3102, 'BUTTON', 'asset:flow:delete',   '删除流程', NULL, NULL, NULL, 4),
(3125, 3102, 'BUTTON', 'asset:flow:validate', '校验流程', NULL, NULL, NULL, 5);

UPDATE sys_permission SET name_tw = '編輯流程', name_en = 'Edit flow'   WHERE id = 3123;
UPDATE sys_permission SET name_tw = '刪除流程', name_en = 'Delete flow' WHERE id = 3124;
UPDATE sys_permission SET name_tw = '校驗流程', name_en = 'Validate flow' WHERE id = 3125;

-- 授权：架构师与开发岗可编辑校验；租户超管全部
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(710001, 2002, 3123), (710002, 2002, 3124), (710003, 2002, 3125),
(710011, 2003, 3123), (710012, 2003, 3125),
(710021, 2101, 3123), (710022, 2101, 3124), (710023, 2101, 3125),
(710031, 2102, 3123), (710032, 2102, 3125);
