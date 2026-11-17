-- ============================================================================
-- 国际化：权限菜单名称支持繁体中文与英文
-- 前端切换语言后重新拉取 /auth/me，菜单名即为对应语言
-- ============================================================================

ALTER TABLE sys_permission ADD COLUMN name_tw varchar(64);
ALTER TABLE sys_permission ADD COLUMN name_en varchar(64);
COMMENT ON COLUMN sys_permission.name_tw IS '繁体中文名称';
COMMENT ON COLUMN sys_permission.name_en IS '英文名称';

UPDATE sys_permission SET name_tw = '概覽',       name_en = 'Overview'          WHERE id = 3001;
UPDATE sys_permission SET name_tw = '整合資產',   name_en = 'Integration'       WHERE id = 3100;
UPDATE sys_permission SET name_tw = 'API 資產',   name_en = 'API Assets'        WHERE id = 3101;
UPDATE sys_permission SET name_tw = '流程編排',   name_en = 'Flow Designer'     WHERE id = 3102;
UPDATE sys_permission SET name_tw = '連接管理',   name_en = 'Connections'       WHERE id = 3103;
UPDATE sys_permission SET name_tw = '運行',       name_en = 'Operations'        WHERE id = 3200;
UPDATE sys_permission SET name_tw = '整合作戰室', name_en = 'Command Center'    WHERE id = 3201;
UPDATE sys_permission SET name_tw = '死信處置',   name_en = 'Dead Letters'      WHERE id = 3202;
UPDATE sys_permission SET name_tw = '審計日誌',   name_en = 'Audit Logs'        WHERE id = 3203;
UPDATE sys_permission SET name_tw = '系統管理',   name_en = 'System'            WHERE id = 3300;
UPDATE sys_permission SET name_tw = '用戶管理',   name_en = 'Users'             WHERE id = 3301;
UPDATE sys_permission SET name_tw = '角色管理',   name_en = 'Roles'             WHERE id = 3302;
UPDATE sys_permission SET name_tw = '租戶管理',   name_en = 'Tenants'           WHERE id = 3303;
