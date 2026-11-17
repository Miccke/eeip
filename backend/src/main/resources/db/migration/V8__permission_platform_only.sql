-- ============================================================================
-- 修正：平台级权限点不应授予租户
-- 背景：开通租户时"授予全部权限"会把「租户管理」也发出去，
--       导致合作方租户的管理员能看到并调用平台级接口。
-- ============================================================================

ALTER TABLE sys_permission ADD COLUMN platform_only boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN sys_permission.platform_only IS '平台级权限：仅集团平台租户可用，不得授予其他租户';

UPDATE sys_permission SET platform_only = true WHERE code LIKE 'system:tenant:%';

-- 清理已经误授出去的平台级权限（历史租户角色）
DELETE FROM sys_role_permission
WHERE permission_id IN (SELECT id FROM sys_permission WHERE platform_only = true)
  AND role_id IN (SELECT id FROM sys_role WHERE tenant_id <> 1);
