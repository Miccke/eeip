-- ============================================================================
-- 三方授权：应用（调用方）注册、接口授权、MD5 签名
-- 场景：B 系统注册接口 → 平台把接口授权给 A 系统（appKey/appSecret）
--       A 系统用签名换 JWT → 携带 JWT + 签名调用接口
-- ============================================================================

CREATE TABLE t_app_client (
    id          bigint       PRIMARY KEY,
    tenant_id   bigint       NOT NULL DEFAULT 1,
    app_key     varchar(64)  NOT NULL,
    app_secret  varchar(128) NOT NULL,      -- 演示阶段明文；生产接 KMS
    app_name    varchar(128) NOT NULL,
    owner       varchar(64),
    status      smallint     NOT NULL DEFAULT 1,
    expire_at   timestamptz,
    allowed_ips varchar(512),
    remark      varchar(256),
    created_by  varchar(64),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  varchar(64),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false,
    version_lock int         NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_app_client_key ON t_app_client (tenant_id, app_key) WHERE deleted = false;
COMMENT ON TABLE t_app_client IS '接入应用（调用方）：appKey + appSecret';
COMMENT ON COLUMN t_app_client.app_secret IS '签名密钥，演示阶段明文存储';

CREATE TABLE t_api_grant (
    id         bigint      PRIMARY KEY,
    tenant_id  bigint      NOT NULL DEFAULT 1,
    client_id  bigint      NOT NULL,
    api_id     bigint      NOT NULL,
    status     smallint    NOT NULL DEFAULT 1,
    qps        int         NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted    boolean     NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_api_grant ON t_api_grant (client_id, api_id) WHERE deleted = false;
COMMENT ON TABLE t_api_grant IS '接口授权：应用可以调用哪些对外接口';

-- 权限点：应用授权（系统管理下）
INSERT INTO sys_permission (id, parent_id, type, code, name, path, component, icon, sort) VALUES
(3304, 3300, 'MENU',   'system:app:list',   '应用授权', '/system/app-client', 'system/app-client/index', 'key', 4),
(3341, 3304, 'BUTTON', 'system:app:create', '创建应用', NULL, NULL, NULL, 1),
(3342, 3304, 'BUTTON', 'system:app:update', '编辑应用', NULL, NULL, NULL, 2),
(3343, 3304, 'BUTTON', 'system:app:delete', '删除应用', NULL, NULL, NULL, 3),
(3344, 3304, 'BUTTON', 'system:app:grant',  '接口授权', NULL, NULL, NULL, 4);

UPDATE sys_permission SET name_tw = '應用授權', name_en = 'App Access'      WHERE id = 3304;
UPDATE sys_permission SET name_tw = '建立應用', name_en = 'Create app'      WHERE id = 3341;
UPDATE sys_permission SET name_tw = '編輯應用', name_en = 'Edit app'        WHERE id = 3342;
UPDATE sys_permission SET name_tw = '刪除應用', name_en = 'Delete app'      WHERE id = 3343;
UPDATE sys_permission SET name_tw = '介面授權', name_en = 'Grant APIs'      WHERE id = 3344;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(730001, 2001, 3304), (730002, 2001, 3341), (730003, 2001, 3342), (730004, 2001, 3343), (730005, 2001, 3344),
(730011, 2101, 3304), (730012, 2101, 3341), (730013, 2101, 3342), (730014, 2101, 3344);
