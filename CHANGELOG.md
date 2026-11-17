# 变更日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)：`主版本.次版本.修订号`。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## [Unreleased]

### 计划中
- 流程 DSL 定义与校验（JSON Schema + 环检测）
- 流程执行引擎 v0（串行节点、实例落库、调试接口）
- 编排画布（拖拽连线、属性面板）

---

## [0.5.0] - 2026-11-17

> 主题：**权限与治理地基**
> 一句话：从"能登录"到"能治理"——权限制约到数据行，租户隔离到 SQL，操作留痕到审计表，界面支持三种语言。

### 新增

#### 多租户（S4）
- `sys_tenant` 租户表；登录按租户编码识别，`TenantContext` 贯穿请求
- MyBatis-Plus `TenantLineInnerInterceptor` 自动追加 `tenant_id`，业务代码零感知
- 平台级表（`sys_tenant`、`sys_permission`）与关联表显式排除
- **一键开通租户**：`POST /api/v1/system/tenants/provision` 一次调用建租户 + 2 个角色 + 管理员 + 授权，初始密码仅返回一次
- 权限点新增 `platform_only` 标记，租户管理员无法获得「租户管理」等平台能力

#### 操作审计（S2）
- `sys_audit_log` 表 + `@AuditLog` 注解 + AOP 切面
- 记录操作人、参数（`password/secret/token` 自动脱敏为 `******`）、结果、耗时、IP、UA
- 独立线程池异步写入，审计异常不影响主流程
- 查询接口 `GET /api/v1/ops/audit-logs`（操作人/动作/结果/时间范围）+ 动作字典

#### 数据权限（S3）
- `DataScopeType`：SELF / PROJECT / TENANT / ALL，多角色取最宽
- `@DataScope` 注解 + `DataPermissionInterceptor` 在分页前改写 SQL
- `LoginUser` 缓存 `deptName` / `dataScope`，SQL 改写不再查库

#### HTTP 连接器（S5）
- `t_connection` 表；`HttpConnector` 支持 GET/POST/PUT/DELETE，四种认证
- **SSRF 防护**：默认拒绝回环、内网、链路本地地址
- **凭证只进不出**：VO 只返回 `hasAuth`
- 连通性测试 `POST /{id}/test` 与保存前预检 `POST /test-draft`

#### 国际化
- 前端 vue-i18n：简体中文 / 繁體中文 / English，头部一键切换并持久化
- 后端 MessageSource：错误文案、校验提示、菜单名称按 `Accept-Language` 返回
- Ant Design Vue 与 dayjs 语言包随语言切换

#### 工程
- 统一响应 `R<T>`、全局异常、Flyway 迁移（V1–V9）、SpringDoc 字段级文档
- 前端暗色主题与 AntDV token 对齐；`v-hasPerm` 指令；动态菜单

### 修复

| 缺陷 | 影响 | 根因 | 修复 |
| --- | --- | --- | --- |
| 乐观锁未启用 | **所有编辑接口 500**（用户/角色/连接） | 实体有 `@Version` 但未注册 `OptimisticLockerInnerInterceptor`，`updateById` 缺参数抛 `BindingException` | 拦截器链补乐观锁（多租户 → 数据权限 → 乐观锁 → 分页） |
| 雪花 ID 精度丢失 | 前端按 ID 操作提示"连接不存在" | 19 位 Long 超出 JS `Number.MAX_SAFE_INTEGER` | Jackson 全局把 Long 序列化为字符串；前端 `ID = string` |
| 平台权限越权 | 合作方租户超管可调用 `/system/tenants` | 开通租户时"授予全部权限"包含平台级权限 | 权限点加 `platform_only`，开通时只授予可授予集合 |
| 连接测试残留错误 | 测试成功后仍显示上次的错误原因 | `updateById` 跳过 null 字段 | 改用 `UpdateWrapper` 显式 set |
| 静态资源兜底吞路由 | `POST /x:action` 形式端点 404 | Spring 把 `:action` 当静态路径 | 统一改为嵌套路径 |
| 循环依赖 | 应用启动失败 | `SecurityConfig` 提供 `PasswordEncoder`，被认证服务依赖 | 拆出 `CryptoConfig` |

### 验收证据

| 场景 | 结果 | 截图 |
| --- | --- | --- |
| 五层鉴权 | `dev01` 菜单缺省、路由弹回、接口 403、按钮消失、数据 6 条 | [s3](screens/s3-users-dept-scope.png) |
| 数据范围 | ALL 9 / TENANT 9 / PROJECT 6 / SELF 1 | [s3](screens/s3-users-self-scope.png) |
| 租户隔离 | 两个同名 `admin` 互不可见（9 人 vs 2 人） | [s4](screens/s4-users-supplier.png) |
| 审计 | 三条记录含脱敏参数与失败留痕 | [s2](screens/s2-audit-admin.png) |
| 连接器 | GitHub 公开 API：200 / 989ms | [s5](screens/s5-connection-test.png) |
| 国际化 | 中/繁/英三语界面 + 后端文案 | [en](screens/i18n-user-en-US.png) · [繁](screens/i18n-user-zh-TW.png) |

---

## [0.1.0] - 2026-11-17

> 主题：**环境与 RBAC 闭环**

### 新增
- Docker 中间件（PostgreSQL 16 / Redis 7 / Adminer）与便携 JDK 17 + Maven 3.9
- Spring Boot 3.3 骨架：统一响应、全局异常、MyBatis-Plus、Flyway
- RBAC 数据模型（6 张表）与种子数据；JWT + Redis 登录态
- 前端 Vue 3 + Ant Design Vue：登录页、暗色 Layout、动态菜单、路由守卫、按钮权限
- 用户管理、角色管理（含权限树分配）页面

### 验收证据
- [登录](screens/s1-login.png) · [概览](screens/s1-dashboard-admin.png) · [用户](screens/s1-users-admin.png) · [角色](screens/s1-roles-admin.png)

---

## 文档版本

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| v1.2 | 2026-11-17 | 安装 UI Skill，产出界面原型 6 页 + 7 张原型图 |
| v1.1 | 2026-11-17 | 技术栈锁定，新增详细设计文档包 14 份 |
| v1.0 | 2026-11-17 | 需求分析文档包 15 份定稿 |
