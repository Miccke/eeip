-- ============================================================================
-- EEIP · RBAC 基础模型（V1）
-- 说明：单体多租户预留 tenant_id（默认 1），当前不启用租户隔离插件
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------------
CREATE TABLE sys_user (
    id            bigint       PRIMARY KEY,
    tenant_id     bigint       NOT NULL DEFAULT 1,
    username      varchar(64)  NOT NULL,
    password      varchar(100) NOT NULL,
    nickname      varchar(64)  NOT NULL,
    email         varchar(128),
    phone         varchar(32),
    avatar        varchar(256),
    dept_name     varchar(64),
    status        smallint     NOT NULL DEFAULT 1,   -- 1 启用 / 0 停用
    last_login_at timestamptz,
    last_login_ip varchar(64),
    remark        varchar(256),
    created_by    varchar(64),
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    varchar(64),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false,
    version       int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_sys_user_username ON sys_user (tenant_id, username) WHERE deleted = false;
COMMENT ON TABLE sys_user IS '用户';

-- ---------------------------------------------------------------------------
-- 角色
-- ---------------------------------------------------------------------------
CREATE TABLE sys_role (
    id          bigint       PRIMARY KEY,
    tenant_id   bigint       NOT NULL DEFAULT 1,
    code        varchar(64)  NOT NULL,      -- SUPER_ADMIN / DEVELOPER ...
    name        varchar(64)  NOT NULL,
    data_scope  varchar(16)  NOT NULL DEFAULT 'SELF',  -- ALL / TENANT / PROJECT / SELF
    sort        int          NOT NULL DEFAULT 0,
    status      smallint     NOT NULL DEFAULT 1,
    builtin     boolean      NOT NULL DEFAULT false,   -- 内置角色不可删除
    remark      varchar(256),
    created_by  varchar(64),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  varchar(64),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false,
    version     int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_sys_role_code ON sys_role (tenant_id, code) WHERE deleted = false;
COMMENT ON TABLE sys_role IS '角色';

-- ---------------------------------------------------------------------------
-- 权限（目录 / 菜单 / 按钮 三合一树）
-- ---------------------------------------------------------------------------
CREATE TABLE sys_permission (
    id          bigint       PRIMARY KEY,
    tenant_id   bigint       NOT NULL DEFAULT 1,
    parent_id   bigint       NOT NULL DEFAULT 0,
    type        varchar(16)  NOT NULL,      -- DIR 目录 / MENU 菜单 / BUTTON 按钮
    code        varchar(128),               -- 权限标识，如 system:user:add；DIR 可为空
    name        varchar(64)  NOT NULL,
    path        varchar(128),               -- 前端路由
    component   varchar(128),               -- 前端组件路径
    icon        varchar(64),
    sort        int          NOT NULL DEFAULT 0,
    hidden      boolean      NOT NULL DEFAULT false,
    status      smallint     NOT NULL DEFAULT 1,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE INDEX ix_sys_perm_parent ON sys_permission (parent_id, sort);
CREATE UNIQUE INDEX ux_sys_perm_code ON sys_permission (code) WHERE code IS NOT NULL AND deleted = false;
COMMENT ON TABLE sys_permission IS '权限点：目录/菜单/按钮';

-- ---------------------------------------------------------------------------
-- 关联表
-- ---------------------------------------------------------------------------
CREATE TABLE sys_user_role (
    id         bigint      PRIMARY KEY,
    user_id    bigint      NOT NULL,
    role_id    bigint      NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_sys_user_role ON sys_user_role (user_id, role_id);

CREATE TABLE sys_role_permission (
    id            bigint      PRIMARY KEY,
    role_id       bigint      NOT NULL,
    permission_id bigint      NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_sys_role_perm ON sys_role_permission (role_id, permission_id);

-- ---------------------------------------------------------------------------
-- 登录日志
-- ---------------------------------------------------------------------------
CREATE TABLE sys_login_log (
    id         bigint      PRIMARY KEY,
    tenant_id  bigint      NOT NULL DEFAULT 1,
    username   varchar(64) NOT NULL,
    ip         varchar(64),
    user_agent varchar(256),
    success    boolean     NOT NULL,
    message    varchar(256),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_sys_login_log_time ON sys_login_log (created_at DESC);
COMMENT ON TABLE sys_login_log IS '登录日志';
