-- ============================================================================
-- S13 · 实例监控：查询索引 + 运行记录菜单
-- ============================================================================

-- 按状态与时间查（监控页主查询）
CREATE INDEX IF NOT EXISTS ix_instance_status ON t_flow_instance (status, started_at DESC)
    WHERE deleted = false;
CREATE INDEX IF NOT EXISTS ix_instance_started ON t_flow_instance (started_at DESC)
    WHERE deleted = false;

-- 节点轨迹按实例 + 顺序取（瀑布图）
CREATE INDEX IF NOT EXISTS ix_inst_node ON t_flow_instance_node (instance_id, seq);

-- 菜单：运行 → 运行记录
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3204, 3200, 'MENU', 'run:instance:list', '运行记录', '/ops/instances', 'ops/instances/index', 'history', 30);
UPDATE sys_permission SET name_tw = '運行記錄', name_en = 'Instances' WHERE id = 3204;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(780001, 2002, 3204), (780011, 2003, 3204), (780021, 2101, 3204);

-- 排序微调：运行记录排在最前
UPDATE sys_permission SET sort = 34 WHERE id = 3201;
UPDATE sys_permission SET sort = 35 WHERE id = 3202;
UPDATE sys_permission SET sort = 36 WHERE id = 3203;
