-- ============================================================================
-- 领域模型重构：系统（生产方）→ 连接器 → 接口（带版本）→ 对外发布
--
-- 决策（2026-11-18）：
--   1) 一个接口版本可绑定多个连接器（主/备、多环境）
--   2) 系统带环境维度（DEV/TEST/PROD），唯一键含 env
--   3) 对外路径自动生成并带版本：/api/open/v{version}/{systemCode}/{apiCode}
--   4) 存量数据统一归入默认系统 SYS_DEFAULT
-- ============================================================================

-- ── 1. 系统（生产方）────────────────────────────────────
CREATE TABLE t_system (
    id          bigint       PRIMARY KEY,
    tenant_id   bigint       NOT NULL DEFAULT 1,
    code        varchar(64)  NOT NULL,
    name        varchar(128) NOT NULL,
    env         varchar(16)  NOT NULL DEFAULT 'PROD',   -- DEV/TEST/PROD
    protocol    varchar(16)  NOT NULL DEFAULT 'HTTP',   -- 主协议
    base_url    varchar(256),
    owner       varchar(64),
    owner_phone varchar(32),
    status      smallint     NOT NULL DEFAULT 1,
    remark      varchar(256),
    created_by  varchar(64),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  varchar(64),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false,
    version_lock int         NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_system_code ON t_system (tenant_id, code, env) WHERE deleted = false;
COMMENT ON TABLE t_system IS '生产方系统台账';

-- ── 2. 连接器归属系统 ───────────────────────────────────
ALTER TABLE t_connection ADD COLUMN system_code varchar(64);
ALTER TABLE t_connection ADD COLUMN protocol varchar(16);
COMMENT ON COLUMN t_connection.system_code IS '所属生产方系统编码';
COMMENT ON COLUMN t_connection.protocol IS '协议：HTTP/SOAP/JDBC/MQ/FILE';
CREATE INDEX ix_conn_system ON t_connection (system_code) WHERE deleted = false;

-- ── 3. 接口台账（身份，不含具体契约）────────────────────
CREATE TABLE t_api (
    id                 bigint       PRIMARY KEY,
    tenant_id          bigint       NOT NULL DEFAULT 1,
    system_code        varchar(64)  NOT NULL,
    code               varchar(64)  NOT NULL,
    name               varchar(128) NOT NULL,
    status             varchar(16)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT/PUBLISHED/OFFLINE
    current_version_id bigint,
    description        varchar(1024),
    created_by         varchar(64),
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_by         varchar(64),
    updated_at         timestamptz  NOT NULL DEFAULT now(),
    deleted            boolean      NOT NULL DEFAULT false,
    version_lock       int          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_api_code ON t_api (tenant_id, system_code, code) WHERE deleted = false;
COMMENT ON TABLE t_api IS '接口台账：某系统提供了哪些能力';

-- ── 4. 接口版本（契约：方法/路径/报文）──────────────────
CREATE TABLE t_api_version (
    id           bigint       PRIMARY KEY,
    tenant_id    bigint       NOT NULL DEFAULT 1,
    api_id       bigint       NOT NULL,
    version      varchar(16)  NOT NULL,          -- 1.0.0 / 1.1.0
    method       varchar(8)   NOT NULL DEFAULT 'GET',
    path         varchar(512) NOT NULL,          -- 上游路径
    input_schema text,
    output_schema text,
    status       varchar(16)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT/PUBLISHED/DEPRECATED
    change_log   varchar(2048),
    published_at timestamptz,
    published_by varchar(64),
    created_by   varchar(64),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_api_version ON t_api_version (api_id, version) WHERE deleted = false;
COMMENT ON TABLE t_api_version IS '接口契约版本';

-- ── 5. 接口版本 ↔ 连接器（一对多）──────────────────────
CREATE TABLE t_api_connector (
    id             bigint      PRIMARY KEY,
    tenant_id      bigint      NOT NULL DEFAULT 1,
    api_version_id bigint      NOT NULL,
    connector_code varchar(64) NOT NULL,
    role           varchar(8)  NOT NULL DEFAULT 'MAIN',  -- MAIN/BACKUP
    weight         int         NOT NULL DEFAULT 1,
    status         smallint    NOT NULL DEFAULT 1,
    created_at     timestamptz NOT NULL DEFAULT now(),
    deleted        boolean     NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_api_conn ON t_api_connector (api_version_id, connector_code) WHERE deleted = false;
COMMENT ON TABLE t_api_connector IS '接口版本绑定的连接器（主/备）';

-- ── 6. 对外发布：瘦身为"发布记录"，路径自动生成 ─────────
ALTER TABLE t_api_expose ADD COLUMN api_id bigint;
ALTER TABLE t_api_expose ADD COLUMN api_version_id bigint;
ALTER TABLE t_api_expose ADD COLUMN public_path varchar(256);
COMMENT ON COLUMN t_api_expose.public_path IS '自动生成：/api/open/v{ver}/{systemCode}/{apiCode}';

-- ── 7. 存量数据迁移：统一进默认系统 ─────────────────────
INSERT INTO t_system (id, tenant_id, code, name, env, protocol, base_url, owner, status, remark)
VALUES (9001, 1, 'SYS_DEFAULT', '默认系统', 'PROD', 'HTTP', NULL, 'admin', 1,
        '模型重构前的存量连接与接口统一归入此系统');

UPDATE t_connection
SET system_code = 'SYS_DEFAULT',
    protocol = COALESCE(protocol, type, 'HTTP')
WHERE system_code IS NULL;

-- 每条已有对外接口 → 一条接口台账 + 一个 1.0.0 契约版本
INSERT INTO t_api (id, tenant_id, system_code, code, name, status, description)
SELECT id + 100000, tenant_id, 'SYS_DEFAULT', code, name, 'PUBLISHED', remark
FROM t_api_expose WHERE deleted = false;

INSERT INTO t_api_version (id, tenant_id, api_id, version, method, path, status,
                           change_log, published_at, published_by, created_at)
SELECT id + 200000, tenant_id, id + 100000, '1.0.0', method,
       COALESCE(upstream_path, path), 'PUBLISHED', '模型重构迁移生成', now(), 'system', now()
FROM t_api_expose WHERE deleted = false;

INSERT INTO t_api_connector (id, tenant_id, api_version_id, connector_code, role, status)
SELECT id + 300000, tenant_id, id + 200000, connection_code, 'MAIN', 1
FROM t_api_expose WHERE deleted = false AND connection_code IS NOT NULL;

UPDATE t_api_expose e
SET api_id = e.id + 100000,
    api_version_id = e.id + 200000,
    public_path = '/api/open/v1/SYS_DEFAULT/' || e.code
WHERE e.deleted = false;

-- ── 8. 权限：系统注册 ───────────────────────────────────
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3104, 3100, 'MENU',   'asset:system:list',   '系统注册', '/asset/system', 'asset/system/index', 'cluster', 20),
(3351, 3104, 'BUTTON', 'asset:system:create', '注册系统', NULL, NULL, NULL, 1),
(3352, 3104, 'BUTTON', 'asset:system:update', '编辑系统', NULL, NULL, NULL, 2),
(3353, 3104, 'BUTTON', 'asset:system:delete', '删除系统', NULL, NULL, NULL, 3);

UPDATE sys_permission SET name_tw = '系統註冊', name_en = 'Systems'    WHERE id = 3104;
UPDATE sys_permission SET name_tw = '註冊系統', name_en = 'Add system' WHERE id = 3351;
UPDATE sys_permission SET name_tw = '編輯系統', name_en = 'Edit system' WHERE id = 3352;
UPDATE sys_permission SET name_tw = '刪除系統', name_en = 'Delete system' WHERE id = 3353;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(750001, 2002, 3104), (750002, 2002, 3351), (750003, 2002, 3352), (750004, 2002, 3353),
(750011, 2003, 3104),
(750021, 2101, 3104), (750022, 2101, 3351), (750023, 2101, 3352);
