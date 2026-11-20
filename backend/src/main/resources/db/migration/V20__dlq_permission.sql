-- 死信处置权限：重投/丢弃（id 3221 已被 ops:dlq:replay 占用，改用 3222）
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3222, 3202, 'BUTTON', 'ops:dlq:retry', '死信重投', NULL, NULL, NULL, 2);
UPDATE sys_permission SET name_tw = '死信重投', name_en = 'Retry dead letter' WHERE id = 3222;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(790001, 2002, 3222), (790011, 2003, 3222), (790021, 2101, 3222);
