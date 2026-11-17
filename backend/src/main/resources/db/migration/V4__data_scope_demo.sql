-- ============================================================================
-- S3 · 数据范围演示数据
-- 目的：造出两个不同部门的账号，用于验证 PROJECT（同部门）/ SELF（仅本人）范围
-- ============================================================================

-- 自定义角色：数据范围 = 本项目（当前以"同部门"近似项目组）
INSERT INTO sys_role (id, tenant_id, code, name, data_scope, sort, builtin, remark) VALUES
(2006, 1, 'ORDER_DEV', '订单组开发', 'PROJECT', 6, false, '演示数据范围：只看本部门');

-- 两个"业务/订单组"的账号
INSERT INTO sys_user (id, tenant_id, username, password, nickname, email, dept_name, status, created_by) VALUES
(1004, 1, 'order01', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '孙订单', 'order01@eeip.local', '业务/订单组', 1, 'admin'),
(1005, 1, 'order02', '$2a$10$mg3Dtrrih25Q4FxEOdCxkuxRMkMDp3VEumGAcSzrjAyOWZTzJCT3i', '周订单', 'order02@eeip.local', '业务/订单组', 1, 'admin');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(4004, 1004, 2006),
(4005, 1005, 2006);

-- 订单组开发：概览 + API 资产（只读）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(550001, 2006, 3001),
(550002, 2006, 3100),
(550003, 2006, 3101);
