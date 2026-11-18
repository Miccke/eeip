# 变更日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)：`主版本.次版本.修订号`。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## [0.6.1] - 2026-11-18

> 主题：**流程编排器 1:1 对齐原型**
> 原实现是「列表 + JSON 文本框 + 只读预览」，与原型 `docs/prototype/02-flow-designer.html` 差距过大；本轮按原型重做为完整可视化编排器。

### 新增
- **设计器页面** `/asset/flow/:id`，三栏布局 1:1 对齐原型：
  - 左：调色板 180px，5 组分类（触发 / 转换 / 路由 / 调用 / 控制），未支持类型置灰
  - 中：画布（20px 网格、分层配色节点、端口、贝塞尔连线、分支标签）+ 底部调试台（输入/输出/节点耗时）
  - 右：属性面板 280px，基础 / 输入 / 异常三个页签，按节点类型渲染字段
- **交互**：调色板点击加节点、节点拖拽移动、端口拖拽连线、CHOICE 分支增删、节点删除、画布缩放（50%–160%）
- **校验可视化**：校验后问题节点在画布上就地标红（错误）/ 标黄（告警）
- 节点坐标写入 DSL（`position`），保存后布局不丢

### 变更
- 列表页「编辑」改为进入设计器，新建流程直接打开空白设计器（自动带 START + END）
- DSL 编辑从 JSON 文本框改为表单化配置，JSON 作为「输入」页签保留用于高级编辑

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 子路由被守卫拦截 | 打开设计器被弹回首页 | `canAccess` 只做精确匹配，未处理父菜单下的子路由 |
| `/asset/flow/new` 报 500 | 新建流程加载失败 | 仅用 query 判断新建，直接访问路径时把 `new` 当 ID 查库 |
| 设计器模板少闭合标签 | 页面白屏 + 编译报错 | 抽 `board-wrap` 包裹层时漏了一个 `</div>` |

### 验收
```
列表 → 进入设计器 → 画布渲染 4 节点
调色板加点      2 → 4 节点
端口拖拽连线    生成 1 条边
校验            控制台输出 valid/nodeCount/dslHash/errors，问题节点标红
```
截图：`docs/screenshots/s7-designer*.png`（本地）

---

## [0.6.0] - 2026-11-18

> 主题：**流程 DSL 与校验**
> 一句话：能把一条集成流程"描述出来、画出来、校验住"——发布前拦住环、断链和配置缺失。

### 新增

#### 流程定义与版本
- `t_flow` / `t_flow_version`：元数据与 DSL 分离，DSL 存 `jsonb`，附 SHA-256 摘要（运行时缓存 key）
- 版本语义：首版 `1.0.0`，每次编辑生成新草稿 `1.1.0`，发布后旧版本置 `DEPRECATED`
- 接口：列表 / 详情 / 新建 / 编辑 / 删除 / 发布 / 下线 / 版本历史

#### DSL 校验器（本轮核心）
| 检查 | 错误码 | 说明 |
| --- | --- | --- |
| 结构 | `FLOW_NODE_DUP` `FLOW_NODE_TYPE` | 节点 ID 唯一、类型合法 |
| 端点 | `FLOW_START_COUNT` `FLOW_END_COUNT` | 有且仅有 1 个 START，至少 1 个 END |
| 引用 | `FLOW_REF_MISSING` | `next` / `branches[].next` 指向的节点必须存在 |
| **环检测** | `FLOW_CYCLE` | DFS 三色标记，输出完整环路径（如 `n2 → n3 → n2`） |
| 可达性 | `FLOW_UNREACHABLE` | 从 START 出发 BFS，不可达节点给告警 |
| 节点配置 | `FLOW_HTTP_TARGET` `FLOW_TRANSFORM_MAPPING` `FLOW_CHOICE_EMPTY` | 按类型校验必填配置 |

- 发布强校验：校验不通过直接拒绝，错误信息带具体节点
- 校验可单独调用（`POST /flows/validate`），编辑器实时用

#### 本地 Mock 服务
- `/api/mock/**` 免认证，提供 `echo` / `orders/{no}` / `status/{code}` / `delay` / `flaky`
- 目的是让连接器与流程演示不依赖外网（`httpbin` 曾出现 8s 超时）

#### 前端
- 流程编排页：列表、编辑、校验、发布、下线、删除
- 编辑器：DSL 编辑 + **只读流程图预览**（SVG 分层布局，回边绕右侧绘制，环一眼可见）
- 校验结果面板：错误（红）与告警（黄）分列，带错误码与节点 ID

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| jsonb 写入失败 | 新建流程 500 | PG 把 String 当 varchar，写 jsonb 列报类型不匹配 → 增加 `JsonbTypeHandler`（`Types.OTHER`） |
| 环路径错位 | 报 `n2 → n1 → n2`，实际是 `n2 → n3 → n2` | 栈是 LIFO，迭代顺序是栈顶在前，截环前未反转 |
| 首版版本号跳号 | 第一次发布直接是 `1.1.0` | 发布时对草稿又调用了一次 `nextVersion()`，应沿用草稿自身版本 |
| 校验按钮不可见 | 按钮文字与暗色背景同色 | `type="primary" ghost` 在暗色主题下没有合适的文字色 |

### 验收证据
```
校验（正常流程）  valid=true  nodes=4  hash=c90c8690762e
校验（造环）      FLOW_CYCLE 存在环：n2 → n3 → n2
发布（坏流程）    GW_400_BAD_REQUEST 无法发布：存在环：n2 → n3 → n2
删除（已发布）    GW_409_CONFLICT 流程已发布，请先下线再删除
版本历史          v1.1.0 PUBLISHED(加分流) / v1.0.0 DEPRECATED(首个版本)
权限              dev01 列表 200 / 发布 403
```
截图：`docs/screenshots/s7-flow-list.png`、`s7-flow-editor.png`、`s7-flow-validate.png`（本地）

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

| 场景 | 结果 | 截图（本地 docs/screenshots/） |
| --- | --- | --- |
| 五层鉴权 | `dev01` 菜单缺省、路由弹回、接口 403、按钮消失、数据 6 条 | `docs/screenshots/s3` |
| 数据范围 | ALL 9 / TENANT 9 / PROJECT 6 / SELF 1 | `docs/screenshots/s3` |
| 租户隔离 | 两个同名 `admin` 互不可见（9 人 vs 2 人） | `docs/screenshots/s4` |
| 审计 | 三条记录含脱敏参数与失败留痕 | `docs/screenshots/s2` |
| 连接器 | GitHub 公开 API：200 / 989ms | `docs/screenshots/s5` |
| 国际化 | 中/繁/英三语界面 + 后端文案 | `docs/screenshots/en` · `docs/screenshots/繁` |

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
- 截图：`docs/screenshots/s1-login.png`、`s1-dashboard-admin.png`、`s1-users-admin.png`、`s1-roles-admin.png`（本地）

---

## 文档版本

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| v1.2 | 2026-11-17 | 安装 UI Skill，产出界面原型 6 页 + 7 张原型图 |
| v1.1 | 2026-11-17 | 技术栈锁定，新增详细设计文档包 14 份 |
| v1.0 | 2026-11-17 | 需求分析文档包 15 份定稿 |
