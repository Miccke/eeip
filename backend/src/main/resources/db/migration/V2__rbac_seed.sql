-- ============================================================================
-- EEIP · RBAC 种子数据（V2）
-- 初始账号：admin / Admin@123
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 用户（密码 BCrypt，明文 Admin@123）
-- ---------------------------------------------------------------------------
INSERT INTO sys_user (id, tenant_id, username, password, nickname, email, dept_name, status, remark) VALUES
(1001, 1, 'admin', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '王工', 'admin@eeip.local', 'IT/集成组', 1, '内置管理员'),
(1002, 1, 'dev01', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '李开发', 'dev01@eeip.local', 'IT/集成组', 1, '集成开发工程师'),
(1003, 1, 'ops01', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '赵运维', 'ops01@eeip.local', 'IT/运维组', 1, '运维工程师');

-- ---------------------------------------------------------------------------
-- 角色
-- ---------------------------------------------------------------------------
INSERT INTO sys_role (id, tenant_id, code, name, data_scope, sort, builtin, remark) VALUES
(2001, 1, 'SUPER_ADMIN',  '超级管理员',   'ALL',     1, true,  '拥有全部权限'),
(2002, 1, 'ARCHITECT',    '集成架构师',   'TENANT',  2, true,  '设计、审批、发布'),
(2003, 1, 'DEVELOPER',    '集成开发工程师','PROJECT', 3, true,  '编排与调试'),
(2004, 1, 'OPERATOR',     '运维工程师',   'TENANT',  4, true,  '告警与死信处置'),
(2005, 1, 'AUDITOR',      '安全审计员',   'TENANT',  5, true,  '只读审计，无写权限');

-- ---------------------------------------------------------------------------
-- 权限树：目录(0) / 菜单(1) / 按钮(2)   parent_id
-- ---------------------------------------------------------------------------
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
-- 概览
(3001, 0,    'MENU',   'dashboard:view',   '概览',       '/dashboard',    'dashboard/index',   'dashboard', 10),
-- 集成资产
(3100, 0,    'DIR',    NULL,               '集成资产',   '/asset',        NULL,                'layers',    20),
(3101, 3100, 'MENU',   'asset:api:list',   'API 资产',   '/asset/api',    'asset/api/index',   'api',       21),
(3111, 3101, 'BUTTON', 'asset:api:create', '注册接口',   NULL, NULL, NULL, 1),
(3112, 3101, 'BUTTON', 'asset:api:update', '编辑接口',   NULL, NULL, NULL, 2),
(3113, 3101, 'BUTTON', 'asset:api:offline','下线接口',   NULL, NULL, NULL, 3),
(3102, 3100, 'MENU',   'asset:flow:list',  '流程编排',   '/asset/flow',   'asset/flow/index',  'route',     22),
(3121, 3102, 'BUTTON', 'asset:flow:create','新建流程',   NULL, NULL, NULL, 1),
(3122, 3102, 'BUTTON', 'asset:flow:publish','发布流程',  NULL, NULL, NULL, 2),
-- 运行
(3200, 0,    'DIR',    NULL,               '运行',       '/ops',          NULL,                'activity',  30),
(3201, 3200, 'MENU',   'ops:console:view', '集成作战室', '/ops/console',  'ops/console/index', 'monitor',   31),
(3202, 3200, 'MENU',   'ops:dlq:list',     '死信处置',   '/ops/dlq',      'ops/dlq/index',     'inbox',     32),
(3221, 3202, 'BUTTON', 'ops:dlq:replay',   '重投消息',   NULL, NULL, NULL, 1),
(3203, 3200, 'MENU',   'ops:audit:list',   '审计日志',   '/ops/audit',    'ops/audit/index',   'shield',    33),
-- 系统管理
(3300, 0,    'DIR',    NULL,               '系统管理',   '/system',       NULL,                'settings',  40),
(3301, 3300, 'MENU',   'system:user:list', '用户管理',   '/system/user',  'system/user/index', 'users',     41),
(3311, 3301, 'BUTTON', 'system:user:create','新增用户',  NULL, NULL, NULL, 1),
(3312, 3301, 'BUTTON', 'system:user:update','编辑用户',  NULL, NULL, NULL, 2),
(3313, 3301, 'BUTTON', 'system:user:delete','删除用户',  NULL, NULL, NULL, 3),
(3314, 3301, 'BUTTON', 'system:user:reset', '重置密码',  NULL, NULL, NULL, 4),
(3302, 3300, 'MENU',   'system:role:list', '角色管理',   '/system/role',  'system/role/index', 'key',       42),
(3321, 3302, 'BUTTON', 'system:role:create','新增角色',  NULL, NULL, NULL, 1),
(3322, 3302, 'BUTTON', 'system:role:update','编辑角色',  NULL, NULL, NULL, 2),
(3323, 3302, 'BUTTON', 'system:role:delete','删除角色',  NULL, NULL, NULL, 3),
(3324, 3302, 'BUTTON', 'system:role:assign','分配权限',  NULL, NULL, NULL, 4);

-- ---------------------------------------------------------------------------
-- 用户-角色
-- ---------------------------------------------------------------------------
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(4001, 1001, 2001),
(4002, 1002, 2003),
(4003, 1003, 2004);

-- ---------------------------------------------------------------------------
-- 角色-权限
-- ---------------------------------------------------------------------------
-- 超级管理员：全部
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 500000 + row_number() OVER (ORDER BY id), 2001, id FROM sys_permission;

-- 架构师：概览 + 集成资产全部 + 作战室
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(510001, 2002, 3001), (510002, 2002, 3100), (510003, 2002, 3101), (510004, 2002, 3111),
(510005, 2002, 3112), (510006, 2002, 3113), (510007, 2002, 3102), (510008, 2002, 3121),
(510009, 2002, 3122), (510010, 2002, 3200), (510011, 2002, 3201);

-- 开发工程师：概览 + 集成资产（仅接口查看 + 流程编辑）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(520001, 2003, 3001), (520002, 2003, 3100), (520003, 2003, 3101),
(520004, 2003, 3102), (520005, 2003, 3121);

-- 运维工程师：概览 + 运行全部
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(530001, 2004, 3001), (530002, 2004, 3200), (530003, 2004, 3201),
(530004, 2004, 3202), (530005, 2004, 3221), (530006, 2004, 3203);

-- 审计员：概览 + 审计只读
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(540001, 2005, 3001), (540002, 2005, 3200), (540003, 2005, 3203);
