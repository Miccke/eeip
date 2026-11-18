<div align="center">

# EEIP · 企业级系统集成平台

**把散落在各系统之间的直连，收敛为一条可治理、可追溯的集成底座**

[![CI](https://github.com/Miccke/eeip/actions/workflows/ci.yml/badge.svg)](https://github.com/Miccke/eeip/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.6.0-f2a33c)](CHANGELOG.md)
[![Backend](https://img.shields.io/badge/backend-Spring%20Boot%203.3%20%2B%20MyBatis--Plus-6DB33F)](#技术栈)
[![Frontend](https://img.shields.io/badge/frontend-Vue%203%20%2B%20Ant%20Design%20Vue%204-42B883)](#技术栈)
[![Database](https://img.shields.io/badge/database-PostgreSQL%2016-336791)](#技术栈)
[![i18n](https://img.shields.io/badge/i18n-zh--CN%20%7C%20zh--TW%20%7C%20en--US-39CFBF)](#国际化)
[![License](https://img.shields.io/badge/license-Internal-lightgrey)](#许可)

</div>

---

## 这是什么

EEIP 是一个面向多系统、多云、多协议环境的**统一集成底座**：统一接入、可视化编排、全链路可观测，并对集成资产实施治理。

当前进度：**已建成权限与治理地基**（RBAC 五层鉴权、多租户、操作审计、数据范围、HTTP 连接器、三语国际化），正在向**流程编排引擎**推进。

> 这不是一个 demo 仓库：每一步都有可运行的服务、可验证的截图与可复现的操作路径，见 [CHANGELOG](CHANGELOG.md)。

## 界面

控制台为暗色主题，9 个页面：概览、API 资产、流程编排、连接管理、集成作战室、死信处置、审计日志、用户/角色/租户管理。
截图与原型图保存在本地 `docs/screenshots/` 与 `docs/prototype/`（**不随仓库分发**）。

## 能力

### RBAC 五层鉴权

| 层 | 机制 | 证据 |
| --- | --- | --- |
| 菜单级 | `/auth/me` 按权限返回菜单树 | `dev01` 侧边栏无「系统管理」 |
| 路由级 | 守卫 `canAccess(path)` | 直接输 URL 被弹回首页 |
| 接口级 | `@PreAuthorize("hasAuthority('...')")` | 越权调用返回 403 |
| 按钮级 | `v-hasPerm` 指令 | 无权限按钮不在 DOM 中 |
| **数据级** | `@DataScope` + MyBatis-Plus 拦截器 | 同一接口：9 / 9 / 6 / 1 条 |

### 多租户

- 登录按**租户编码**识别，之后所有 SQL 自动带 `tenant_id`（`TenantLineInnerInterceptor`）
- 平台级表（租户、权限点）与关联表显式排除
- **一键开通**：建租户 → 建内置角色 → 建管理员 → 授权，一次调用完成，初始密码只返回一次
- 平台级权限标记 `platform_only`，合作方租户拿不到「租户管理」

### 操作审计

- `@AuditLog` 注解 + AOP，记录操作人、参数（**自动脱敏**）、结果、耗时、IP
- 独立线程池异步落库，审计失败不影响业务
- 只追加，不提供修改/删除接口

### 连接器

- HTTP 连接器：GET/POST/PUT/DELETE，四种认证（NONE/BEARER/BASIC/API_KEY）
- **SSRF 防护**：默认拒绝回环与内网地址
- **凭证只进不出**：任何接口都不返回原始凭证
- 本地 `/api/mock/**` 提供可造的回显、状态码、延迟与按概率失败，演示不依赖外网

### 流程编排

- 流程定义与 DSL 分离存储（`jsonb` + SHA-256 摘要），版本发布制
- **发布前强校验**：结构、引用完整性、可达性、**环检测**、节点配置
- 报错精确到节点：`FLOW_CYCLE 存在环：n2 → n3 → n2`
- 只读流程图：按层自动布局，回边绕行绘制，环一眼可见

### 国际化

- 三种语言：简体中文 / 繁體中文 / English，前端一键切换
- 后端按 `Accept-Language` 返回**错误文案、校验提示、菜单名称**
- 文案资源：`backend/src/main/resources/i18n/messages*.properties` 与 `frontend/src/locales/*`

## 技术栈

| 层 | 选型 |
| --- | --- |
| 前端 | Vue 3.5 · TypeScript · Vite 6 · Ant Design Vue 4 · Pinia · vue-i18n |
| 后端 | Spring Boot 3.3 · Spring Security · MyBatis-Plus 3.5 · Flyway |
| 数据 | PostgreSQL 16 · Redis 7（全部 Docker） |
| 文档 | SpringDoc OpenAPI 3（字段级 `@Schema`） |

**架构约束**：模块化单体 + 按伸缩特性划分服务；多租户与数据权限在 SQL 层统一处理，业务代码零感知。

## 快速开始

```bash
# ① 中间件（PostgreSQL / Redis / Adminer）
./scripts/start-middleware.sh

# ② 后端 → http://localhost:8080
./scripts/start-backend.sh

# ③ 前端 → http://localhost:5173（另开终端）
./scripts/start-frontend.sh
```

首次启动自动执行 Flyway 迁移与种子数据，**无需手工建库建表**。

| 服务 | 地址 |
| --- | --- |
| 前端控制台 | http://localhost:5173 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| 健康检查 | http://localhost:8080/actuator/health |
| Adminer（DB） | http://localhost:8088 |

> 本机若无 JDK 17：`scripts/env.sh` 会自动使用仓库内 `.tools/` 下的便携 JDK 17 + Maven 3.9（不污染系统环境）。

## 演示账号

密码统一 `Admin@123`，登录时需填**租户编码**。

| 租户 | 账号 | 角色 | 数据范围 | 用户列表可见 |
| --- | --- | --- | --- | :-: |
| `GROUP` | `admin` | 超级管理员 | ALL | 9 人 |
| `GROUP` | `mgr01` | 部门管理员 | PROJECT（同部门） | 6 人 |
| `GROUP` | `mgr02` | 个人管理员 | SELF | 1 人 |
| `GROUP` | `dev01` | 集成开发 | PROJECT | 403 |
| `SUPPLIER_A` | `admin` | 租户管理员 | 按授权 | 2 人 |

## 工程结构

```
ESB/
├── backend/              Spring Boot（common：响应/异常/安全/审计/数据权限/租户/国际化）
│   └── src/main/
│       ├── java/com/eeip/{common,module}
│       └── resources/{db/migration,i18n}
├── frontend/             Vue 3 + Vite（api / stores / router / layouts / views / locales）
├── deploy/               docker-compose（PG / Redis / Adminer）
├── scripts/              env.sh · start-*.sh
├── .github/workflows/    CI（后端构建 / 前端构建 / 三语词条校验）
└── docs/                 ⚠️ 本地文档，已 gitignore，不随仓库分发
    ├── requirements/     需求分析 15 份
    ├── design/           详细设计 14 份
    ├── prototype/        界面原型
    ├── screenshots/      验收截图
    └── skills/           UI Skill 安装记录
```

## 文档

| 文档 | 位置 | 是否进仓库 |
| --- | --- | :-: |
| 开发手册（账号矩阵 / 鉴权 / 迭代计划 / 踩坑） | `docs/DEVELOPMENT.md` | ❌ |
| 需求分析（15 份） | `docs/requirements/` | ❌ |
| 详细设计（14 份） | `docs/design/` | ❌ |
| 界面原型 | `docs/prototype/` | ❌ |
| 版本演进与验收证据 | [CHANGELOG.md](CHANGELOG.md) | ✅ |

> 需求与设计文档属于内部资料，**不随开源仓库分发**，仅保留在本地 `docs/` 目录（已在 `.gitignore` 中）。

## 路线图

| 版本 | 主题 | 状态 |
| --- | --- | --- |
| v0.5 | 权限与治理地基：RBAC / 多租户 / 审计 / 数据范围 / 连接器 / i18n | ✅ 已交付 |
| v0.6 | 流程 DSL 与校验：流程定义、版本发布、环检测、只读流程图、本地 Mock | ✅ 已交付 |
| v0.7 | 执行引擎 v0：串行节点执行、实例落库、调试接口 | 进行中 |
| v0.8 | 编排画布：拖拽连线、属性面板、DSL 保存 | 计划 |
| v0.9 | 映射引擎与运行监控 | 计划 |
| v1.0 | 死信处置、打包发布、生产可用 | 计划 |

## 贡献与约定

- 代码须有**中文注释**：类与公开方法写 Javadoc，复杂逻辑解释"为什么"
- 接口须有**字段级 Swagger 注释**：每个 DTO/VO 字段都要 `@Schema(description, example)`
- 提交遵循 Conventional Commits（`feat` / `fix` / `docs` / `refactor` / `chore`）
- 数据库变更只新增 `V{n}__xxx.sql`，不修改已执行的迁移

## 赞赏支持

如果这个项目帮你省了时间，可以请我喝杯咖啡 ☕

<p align="center">
  <img src="assets/wechat-pay.png" alt="微信赞赏码" width="240" />
</p>

> 赞赏纯属自愿，不影响任何功能。项目的 Roadmap 与代码永久免费。

## 许可

内部项目，未开源发布。第三方组件许可见各自 LICENSE。
