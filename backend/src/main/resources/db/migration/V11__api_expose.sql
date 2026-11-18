-- ============================================================================
-- 接口开放：把内部系统的接口封装后对外提供
-- 场景：各系统接口 → 平台登记/转换/鉴权/限流 → 统一对外暴露
-- ============================================================================

CREATE TABLE t_api_expose (
    id              bigint       PRIMARY KEY,
    tenant_id       bigint       NOT NULL DEFAULT 1,
    code            varchar(64)  NOT NULL,
    name            varchar(128) NOT NULL,
    method          varchar(8)   NOT NULL DEFAULT 'GET',   -- 对外方法
    path            varchar(256) NOT NULL,                 -- 对外路径，挂在 /api/open 下
    target_type     varchar(16)  NOT NULL DEFAULT 'PROXY', -- PROXY 直连转发 / FLOW 走流程
    connection_code varchar(64),                           -- 上游连接（PROXY 时必填）
    upstream_path   varchar(512),                          -- 上游路径模板，支持 {id} 与 $query
    upstream_method varchar(8),                            -- 上游方法，空则沿用对外方法
    headers_json    text,                                  -- 追加请求头
    auth_type       varchar(16)  NOT NULL DEFAULT 'NONE',  -- NONE / API_KEY
    api_key         varchar(128),                          -- 演示阶段明文，后续接 KMS
    qps             int          NOT NULL DEFAULT 0,       -- 0 表示不限
    status          smallint     NOT NULL DEFAULT 1,       -- 1 启用 0 停用
    version         varchar(16)  NOT NULL DEFAULT 'v1',
    remark          varchar(256),
    created_by      varchar(64),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      varchar(64),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    deleted         boolean      NOT NULL DEFAULT false,
    version_lock    int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_api_expose_code ON t_api_expose (tenant_id, code) WHERE deleted = false;
CREATE UNIQUE INDEX ux_api_expose_path ON t_api_expose (tenant_id, method, path) WHERE deleted = false;
CREATE INDEX ix_api_expose_status ON t_api_expose (status) WHERE deleted = false;
COMMENT ON TABLE t_api_expose IS '对外暴露的接口（内部接口的封装与登记）';
COMMENT ON COLUMN t_api_expose.upstream_path IS '上游路径模板，支持 {id} 占位与 query 透传';

-- 权限：接口开放（挂在 API 资产菜单下）
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3114, 3101, 'BUTTON', 'asset:api:publish', '发布接口', NULL, NULL, NULL, 4),
(3115, 3101, 'BUTTON', 'asset:api:delete',  '删除接口', NULL, NULL, NULL, 5),
(3116, 3101, 'BUTTON', 'asset:api:test',    '调试接口', NULL, NULL, NULL, 6);

UPDATE sys_permission SET name_tw = '發佈介面', name_en = 'Publish API' WHERE id = 3114;
UPDATE sys_permission SET name_tw = '刪除介面', name_en = 'Delete API'  WHERE id = 3115;
UPDATE sys_permission SET name_tw = '除錯介面', name_en = 'Test API'    WHERE id = 3116;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(720001, 2002, 3114), (720002, 2002, 3115), (720003, 2002, 3116),
(720011, 2003, 3116),
(720021, 2101, 3114), (720022, 2101, 3115), (720023, 2101, 3116);
