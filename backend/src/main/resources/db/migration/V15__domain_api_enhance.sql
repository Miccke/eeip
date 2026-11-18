-- ============================================================================
-- 领域管理 + 接口多协议 + 接口关联流程 + 对外网关限流
-- ============================================================================

-- ── 1. 业务领域（大型集团口径）──────────────────────────
CREATE TABLE t_domain (
    id         bigint      PRIMARY KEY,
    tenant_id  bigint      NOT NULL DEFAULT 1,
    code       varchar(32) NOT NULL,
    name       varchar(64) NOT NULL,
    name_en    varchar(64),
    sort       int         NOT NULL DEFAULT 0,
    status     smallint    NOT NULL DEFAULT 1,
    remark     varchar(256),
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted    boolean     NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_domain_code ON t_domain (tenant_id, code) WHERE deleted = false;
COMMENT ON TABLE t_domain IS '业务领域（集团口径），系统与接口按领域归类';

INSERT INTO t_domain (id, tenant_id, code, name, name_en, sort) VALUES
(1001, 1, 'FIN',  '财务',     'Finance',      10),
(1002, 1, 'SCM',  '供应链',   'Supply Chain', 20),
(1003, 1, 'MKT',  '市场营销', 'Marketing',    30),
(1004, 1, 'SALE', '销售',     'Sales',        40),
(1005, 1, 'MFG',  '生产制造', 'Manufacturing',50),
(1006, 1, 'HR',   '人力资源', 'HR',           60),
(1007, 1, 'CS',   '客户服务', 'Customer Care',70),
(1008, 1, 'RD',   '研发',     'R&D',          80),
(1009, 1, 'MDM',  '主数据',   'Master Data',  90),
(1010, 1, 'IT',   '信息技术', 'IT',          100),
(1011, 1, 'GRC',  '风控合规', 'GRC',         110);

-- ── 2. 系统增加所属领域 ─────────────────────────────────
ALTER TABLE t_system ADD COLUMN domain_code varchar(32);
CREATE INDEX ix_system_domain ON t_system (domain_code) WHERE deleted = false;
UPDATE t_system SET domain_code = 'IT' WHERE domain_code IS NULL;

-- ── 3. 接口增强：协议、编号、SLA、实现方式（直连/流程）──
ALTER TABLE t_api ADD COLUMN api_no     varchar(32);
ALTER TABLE t_api ADD COLUMN protocol   varchar(16) NOT NULL DEFAULT 'HTTP';
ALTER TABLE t_api ADD COLUMN sla_level  varchar(4)  NOT NULL DEFAULT 'L2';
ALTER TABLE t_api ADD COLUMN domain_code varchar(32);
ALTER TABLE t_api ADD COLUMN target_type varchar(16) NOT NULL DEFAULT 'CONNECTOR';  -- CONNECTOR / FLOW
ALTER TABLE t_api ADD COLUMN flow_code  varchar(64);
COMMENT ON COLUMN t_api.target_type IS '实现方式：CONNECTOR 直连连接器；FLOW 走流程编排';
COMMENT ON COLUMN t_api.flow_code   IS 'target_type=FLOW 时绑定的流程编码';

-- 编号：API-{领域}-{4 位序号}，用 id 取模保证稳定（不能用窗口函数，PG 不允许）
UPDATE t_api
SET api_no = 'API-' || COALESCE(domain_code, 'GEN') || '-' || lpad((id % 10000)::text, 4, '0')
WHERE api_no IS NULL;
UPDATE t_api SET domain_code = (SELECT domain_code FROM t_system s WHERE s.code = t_api.system_code LIMIT 1)
WHERE domain_code IS NULL;

-- ── 4. 对外网关：IP 白名单 + 限流所需字段已有，补 secret 给应用 ──
ALTER TABLE t_app_client ADD COLUMN fail_count int NOT NULL DEFAULT 0;
COMMENT ON COLUMN t_app_client.fail_count IS '连续签名失败次数（超阈值可自动禁用）';

-- ── 5. 权限：领域管理 ───────────────────────────────────
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3105, 3100, 'MENU',   'asset:domain:list',   '领域管理', '/asset/domain', 'asset/domain/index', 'appstore', 19),
(3361, 3105, 'BUTTON', 'asset:domain:create', '新增领域', NULL, NULL, NULL, 1),
(3362, 3105, 'BUTTON', 'asset:domain:update', '编辑领域', NULL, NULL, NULL, 2),
(3363, 3105, 'BUTTON', 'asset:domain:delete', '删除领域', NULL, NULL, NULL, 3);

UPDATE sys_permission SET name_tw = '領域管理', name_en = 'Domains'      WHERE id = 3105;
UPDATE sys_permission SET name_tw = '新增領域', name_en = 'Add domain'   WHERE id = 3361;
UPDATE sys_permission SET name_tw = '編輯領域', name_en = 'Edit domain'  WHERE id = 3362;
UPDATE sys_permission SET name_tw = '刪除領域', name_en = 'Delete domain' WHERE id = 3363;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(760001, 2002, 3105), (760002, 2002, 3361), (760003, 2002, 3362), (760004, 2002, 3363),
(760011, 2003, 3105),
(760021, 2101, 3105), (760022, 2101, 3361);
