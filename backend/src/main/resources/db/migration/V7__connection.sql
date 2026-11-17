-- ============================================================================
-- S5 · 连接管理与 HTTP 连接器
-- t_connection 归属集成域，后续迁入 eeip_meta schema；当前使用 public 并保留 t_ 前缀
-- ============================================================================

CREATE TABLE t_connection (
    id                bigint       PRIMARY KEY,
    tenant_id         bigint       NOT NULL DEFAULT 1,
    code              varchar(64)  NOT NULL,
    name              varchar(128) NOT NULL,
    type              varchar(32)  NOT NULL DEFAULT 'HTTP',
    endpoint          varchar(512) NOT NULL,
    method            varchar(8)   NOT NULL DEFAULT 'GET',
    auth_type         varchar(16)  NOT NULL DEFAULT 'NONE',  -- NONE / BEARER / BASIC / API_KEY
    auth_config       text,                                   -- JSON；演示阶段明文，S6 接入 KMS 后改为引用
    headers_json      text,
    timeout_ms        int          NOT NULL DEFAULT 5000,
    status            smallint     NOT NULL DEFAULT 1,
    last_check_at     timestamptz,
    last_check_status varchar(16),                            -- SUCCESS / FAILED
    last_check_ms     int,
    last_check_error  varchar(512),
    remark            varchar(256),
    created_by        varchar(64),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_by        varchar(64),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    deleted           boolean      NOT NULL DEFAULT false,
    version           int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_conn_code ON t_connection (tenant_id, code) WHERE deleted = false;
COMMENT ON TABLE  t_connection IS '集成连接配置（含凭证，凭证字段不对外返回）';
COMMENT ON COLUMN t_connection.auth_config IS '认证配置 JSON，按 auth_type 不同结构不同';
COMMENT ON COLUMN t_connection.last_check_status IS '最近一次连通性测试结果';

-- 连接管理权限点（挂在"集成资产"目录下）
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3103, 3100, 'MENU',   'asset:connection:list',   '连接管理', '/asset/connection', 'asset/connection/index', 'plug', 23),
(3131, 3103, 'BUTTON', 'asset:connection:create', '新建连接', NULL, NULL, NULL, 1),
(3132, 3103, 'BUTTON', 'asset:connection:update', '编辑连接', NULL, NULL, NULL, 2),
(3133, 3103, 'BUTTON', 'asset:connection:delete', '删除连接', NULL, NULL, NULL, 3),
(3134, 3103, 'BUTTON', 'asset:connection:test',   '连通性测试', NULL, NULL, NULL, 4);

-- 授权：集团架构师全量；开发岗可看可测；供应商侧同样
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(700001, 2002, 3103), (700002, 2002, 3131), (700003, 2002, 3132), (700004, 2002, 3133), (700005, 2002, 3134),
(700011, 2003, 3103), (700012, 2003, 3134),
(700021, 2101, 3103), (700022, 2101, 3131), (700023, 2101, 3132), (700024, 2101, 3133), (700025, 2101, 3134),
(700031, 2102, 3103), (700032, 2102, 3134);
