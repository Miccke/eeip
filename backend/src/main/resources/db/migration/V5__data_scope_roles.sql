-- ============================================================================
-- S3 · 数据范围演示角色与账号
-- 三个账号拥有同样的"用户查看"权限，但数据范围不同，用于验证拦截器
-- ============================================================================

INSERT INTO sys_role (id, tenant_id, code, name, data_scope, sort, builtin, remark) VALUES
(2007, 1, 'DEPT_ADMIN',   '部门管理员', 'PROJECT', 7, false, '只看本部门（演示 PROJECT）'),
(2008, 1, 'SELF_ADMIN',   '个人管理员', 'SELF',    8, false, '只看本人（演示 SELF）'),
(2009, 1, 'TENANT_ADMIN', '租户管理员', 'TENANT',  9, false, '看本租户（演示 TENANT）');

INSERT INTO sys_user (id, tenant_id, username, password, nickname, dept_name, status, created_by) VALUES
(1006, 1, 'mgr01', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '钱部门', 'IT/集成组', 1, 'admin'),
(1007, 1, 'mgr02', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '吴个人', 'IT/集成组', 1, 'admin'),
(1008, 1, 'mgr03', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '郑租户', 'IT/集成组', 1, 'admin');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(4006, 1006, 2007),
(4007, 1007, 2008),
(4008, 1008, 2009);

-- 三个角色都只给「概览 + 系统管理/用户管理（只读）」
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(560001, 2007, 3001), (560002, 2007, 3300), (560003, 2007, 3301),
(570001, 2008, 3001), (570002, 2008, 3300), (570003, 2008, 3301),
(580001, 2009, 3001), (580002, 2009, 3300), (580003, 2009, 3301);
