-- ============================================================================
-- S4 · 多租户
-- 说明：
--   1) sys_tenant 是平台级表，不参与租户过滤
--   2) 业务表通过 tenant_id 隔离，由 MyBatis-Plus 多租户拦截器自动注入
--   3) sys_permission 为全局权限定义（所有租户共用一套权限点），不参与过滤
--   4) 关联表 sys_user_role / sys_role_permission 不含 tenant_id，通过主表间接隔离
-- ============================================================================

CREATE TABLE sys_tenant (
    id           bigint       PRIMARY KEY,
    code         varchar(32)  NOT NULL,
    name         varchar(128) NOT NULL,
    status       smallint     NOT NULL DEFAULT 1,     -- 1 启用 / 0 停用
    tenant_type  varchar(16)  NOT NULL DEFAULT 'INTERNAL', -- INTERNAL 内部 / PARTNER 合作方
    contact_name varchar(64),
    contact_phone varchar(32),
    expire_at    timestamptz,
    remark       varchar(256),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false,
    version      int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_tenant_code ON sys_tenant (code) WHERE deleted = false;
COMMENT ON TABLE sys_tenant IS '租户（平台级表，不参与租户过滤）';
COMMENT ON COLUMN sys_tenant.code IS '租户编码，登录时用于识别租户';

INSERT INTO sys_tenant (id, code, name, tenant_type, contact_name, remark) VALUES
(1, 'GROUP',      '集团总部',   'INTERNAL', '王工', '默认租户，既有账号均属于此租户'),
(2, 'SUPPLIER_A', '供应商A',    'PARTNER',  '李经理', '演示用合作方租户，与集团数据完全隔离');

-- ---------------------------------------------------------------------------
-- 租户 2 的自有角色（与集团同名但独立，ID 不同）
-- ---------------------------------------------------------------------------
INSERT INTO sys_role (id, tenant_id, code, name, data_scope, sort, builtin, remark) VALUES
(2101, 2, 'SUPER_ADMIN', '租户管理员', 'ALL',     1, true, '租户内最高权限'),
(2102, 2, 'DEVELOPER',   '租户开发',   'PROJECT', 3, true, '租户内开发岗');

-- 租户 2 的账号（用户名与集团 admin 相同，靠 tenant_id 区分）
INSERT INTO sys_user (id, tenant_id, username, password, nickname, dept_name, status, created_by) VALUES
(2001, 2, 'admin', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '供应商管理员', '供应商A/IT', 1, 'system'),
(2002, 2, 'dev_a', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '供应商开发',   '供应商A/IT', 1, 'system');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(6001, 2001, 2101),
(6002, 2002, 2102);

-- 租户 2 的角色权限：租户管理员拥有全部权限点
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 600000 + row_number() OVER (ORDER BY id), 2101, id FROM sys_permission;

-- 租户开发：概览 + API 资产 + 系统内用户只读（便于验证租户内数据隔离）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(610001, 2102, 3001),
(610002, 2102, 3100),
(610003, 2102, 3101);

-- ---------------------------------------------------------------------------
-- 新增权限点：租户管理
-- ---------------------------------------------------------------------------
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3303, 3300, 'MENU',   'system:tenant:list',   '租户管理', '/system/tenant', 'system/tenant/index', 'grid', 43),
(3331, 3303, 'BUTTON', 'system:tenant:create', '新增租户', NULL, NULL, NULL, 1),
(3332, 3303, 'BUTTON', 'system:tenant:update', '编辑租户', NULL, NULL, NULL, 2),
(3333, 3303, 'BUTTON', 'system:tenant:delete', '删除租户', NULL, NULL, NULL, 3);

-- 仅授予集团超管（租户管理属于平台运营能力，合作方租户不可见）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(620001, 2001, 3303), (620002, 2001, 3331), (620003, 2001, 3332), (620004, 2001, 3333);
