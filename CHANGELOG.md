# 变更日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)：`主版本.次版本.修订号`。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## [1.7.0] - 2026-11-20

> 主题：**可视化字段映射 —— 不再手写 JSON 表达式**
> 这是整个产品最容易劝退用户的一环，现在有界面了。

### 新增：映射编辑器（`MappingEditor.vue`）
- 三栏布局：**上游样本 + 可用字段** ｜ **目标字段 ← 表达式** ｜ **实时预览**
- 可用字段：递归展开样本（最多 3 层，数组取 `[0]` 路径），点 `▾` 直接选
- 「按样本自动填充」：按样本字段名一键生成映射行
- 双模式：可视化行编辑 ⇄ 原始 JSON，互相同步
- 预览：**逐字段**显示是否取到值（未取到标红），下方给出合并后的目标报文
- 入口：TRANSFORM 节点属性面板「字段映射 → 编辑映射」

### 新增：映射预览接口
- `POST /api/v1/integration/mapping/preview`：`{mapping, input, vars}` → `{output, fields[], error}`
- 求值走 **`TransformExecutor.applyMapping`**（与运行时同一段代码），杜绝"预览对、跑起来错"

### 变更：映射表达式支持简写
- `$input.orderNo`（显式）与 `$.orderNo`（简写，等价上游字段）都支持
- 简写只在首段不是已有变量时生效，避免误伤 `$.nodes.n2`
- 抽取 `TransformExecutor.resolveValue(...)`，执行与预览共用

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 预览字段全判为"未取到" | `$.customerLevel` 取不到值 | 映射语义里 `$` 指向变量根，不是上游报文 → 增加简写归一 |
| 预览值被透传掩盖 | 显示有值其实是上游同名字段 | 逐字段单独求值，不再从合并结果里读 |
| 按钮显示 `designer.editMapping` | 词条加在 `map` 块却用 `designer` 前缀 | 改用 `t('map.editMapping')` |

### 验收
```
mapping: level ← $.customerLevel / amount ← $.body.data.amount.value / orderNo ← $.body.data.orderNo
样本:     { body: { data: { customerLevel: "VIP", amount: { value: 128000.5 }, orderNo: "SO-20261120-009" } } }
预览:     level="VIP" ✓  amount=128000.5 ✓  orderNo="SO-20261120-009" ✓
```

---

## [1.6.0] - 2026-11-20

> 主题：**重试与死信 —— 失败可以被重放，不再只是"红灯"**
> 运行记录从"看板"变成"可操作的控制台"。

### 新增：节点级重试
- 节点配置 `retry: { maxAttempts, delayMs }`，指数固定间隔重试
- 引擎内实现，所有节点通用；`attempts` 落到 `t_flow_instance_node` 与执行轨迹
- 上限保护：`maxAttempts` 夹在 1–10，`delayMs` 夹在 0–60s

### 新增：死信（`t_flow_dead_letter`）
- 实例失败时自动落库：失败节点、错误、原始入参、重试次数
- 唯一索引 `(instance_id)` 保证同一实例只落一条
- 状态机：`PENDING → RETRIED / DISCARDED`
- 接口：
  - `GET /dead-letters`（按状态/流程过滤）
  - `GET /dead-letters/stats`
  - `POST /dead-letters/{id}/retry`：用原始入参重跑当前发布版本，生成新实例
  - `POST /dead-letters/{id}/discard`：标记不再处理（保留记录）
- 幂等：已 `RETRIED`/`DISCARDED` 的死信拒绝重复重投

### 新增：死信处置页（`/ops/dlq`）
- 四张卡：待处理 / 已重投 / 已丢弃 / 累计
- 列表：流程、失败节点、错误、重试次数、状态、产生时间
- 操作：重投（二次确认）、丢弃、详情（原始入参 JSON）
- 权限：`ops:dlq:retry`（重投/丢弃），查看沿用 `ops:dlq:list`

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 迁移失败 | 应用起不来 | V20 用了已占用的权限 id `3221`（`ops:dlq:replay`）→ 改用 `3222` |
| `retry` 配置被拒 | 发布报「未知配置项：retry」 | 节点 Schema `additionalProperties=false` 把引擎保留键也拦了 → 放行 `retry/outputVar/errorPolicy/timeoutMs/onError` |
| 词条重复 | TS1117 构建失败 | `dlq.retried` 既是统计项又是提示语 → 提示语改名 `retriedMsg` |

### 验收
```
流程 retry-demo（HTTP 500 + retry 3 次）
  n1 START       SUCCESS  0ms  attempts=1
  n2 HTTP_INVOKE FAILED 710ms  attempts=3      ← 重试生效
实例 FAILED → 死信 1 条（PENDING, attempts=3, HTTP_STATUS_500）
重投 → 新实例 FAILED，死信状态 RETRIED、retryCount=1、处理人 admin
重复重投 → 409 该死信已处理
丢弃 → DISCARDED，统计 待处理1/已重投1/已丢弃1
```

---

## [1.5.0] - 2026-11-20

> 主题：**实例监控 —— 把"跑了什么、跑得怎样"变成界面**
> 调度器上线后实例持续增长，之前只能 `curl /instances` 看，现在有页面了。

### 新增：运行记录页（`/ops/instances`）
- **四张指标卡**：近 24h 实例数、成功率、平均耗时、失败数
- **筛选**：流程、状态、触发方式（MANUAL/SCHEDULE/HTTP）
- **列表**：实例（短 ID）、流程名+编码、状态、触发方式、耗时、开始时间
- **详情抽屉**（对齐原型 `04-trace`）：
  - 顶部信息条：入口 / 结果 / 耗时 / 节点数 / 开始时间
  - **调用瀑布图**：按开始时间定位、宽度表示耗时，失败节点红色
  - 选中节点面板：节点 ID、耗时、错误、输入/输出 JSON（自动格式化）

### 新增：后端查询能力
- `GET /instances` 增加 `flowCode` / `status` / `triggerType` / `from` / `to` 过滤
- 列表返回 `InstanceVO`，**批量补流程名**（避免 N+1）
- 新增 `GET /instances/stats?hours=24`：总数、成功、失败、运行中、成功率、平均与最大耗时
- 索引：`(status, started_at)`、`(started_at)`、节点轨迹 `(instance_id, seq)`

### 权限
- 新增菜单「运行 → 运行记录」`run:instance:list`（id 3204），三个内置角色默认可见
- 实例查询权限从 `asset:flow:run` 收敛为 `run:instance:list`

### 修复
| 缺陷 | 原因 |
| --- | --- |
| 编译报错 `找不到 getName()` | `IntFlow` 的字段是 `flowName`，我按常规写了 `getName` |

### 验收
```
24h 统计：总 5 / 成功 5 / 成功率 100% / 均耗 165ms
列表：demo-run-order 42ms、cron-demo 216ms（SCHEDULE）
详情：5 节点瀑布，查订单 25ms 为最宽条；点节点看输入输出
```

---

## [1.4.0] - 2026-11-19

> 主题：**调度器 —— 场景 B 闭环（内部定时集成）**
> 流程发布后按 Cron 自动执行，实例与触发记录全部落库。

### 新增：`t_flow_trigger`（触发计划）
- 流程**发布时**扫描 DSL 中的触发器节点，自动生成触发记录；下线时置 `OFFLINE`
- 字段：绑定版本、节点 ID、类型（SCHEDULE/MQ/EVENT/HTTP）、下次触发时间、
  触发次数、错过次数、最近结果与实例 ID
- 唯一键 `(tenant, flow_id, node_id)`；索引 `(next_fire_at)` 支撑到期扫描

### 新增：Cron 扫描与执行
- `@Scheduled` 每 5 秒扫描一次到期触发器（间隔小于最小 cron 粒度，保证准点）
- 执行放到独立线程池（4 线程），**不阻塞调度线程**
- 先推进 `next_fire_at` 再执行：执行失败也不会卡在同一时刻反复触发
- Cron 用 Spring `CronExpression`，支持时区（默认 `Asia/Shanghai`）
- 单次批量 20 条；可通过 `eeip.scheduler.*` 配置开关、间隔、批量

### 新增：触发计划的启停
- `POST /flows/{id}/triggers/{tid}/toggle?active=`：暂停/启用
- 暂停清空下次触发时间，启用按当前 Cron 重算

### 变更：触发节点不再限定 START
- 校验规则从「必须有且仅有 1 个 **START**」改为「必须有且仅有 1 个**触发节点**」
- 判定依据是 manifest 的 `meta.category = trigger` —— 以后加触发器类型不用改校验代码
- 引擎入口同步改为「首个触发节点」；触发器类节点共用同一个执行器

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 暂停后仍按原时间触发 | `next_fire_at` 没被清空 | MyBatis-Plus `updateById` 跳过 null 字段 → 改 `lambdaUpdate().set(...)` 显式写入 |
| 定时流程发布被拒 | 「必须有且仅有 1 个 START 节点，当前 0 个」 | 校验写死 START，不认定时/订阅/事件节点 |
| 词条重复 | `TS1117` 构建失败 | `triggerType` 在 flow 块已存在（触发方式），新键改名 `triggerKind` |
| 变量遮蔽 | `t(...)` 被触发器参数 `t` 覆盖 | 回调参数改名 `trig` |

### 验收
```
流程 cron-demo（*/10 * * * * *）发布 → 触发计划 ACTIVE
21:40:40 / 21:40:50 / 21:41:00 … 每 10 秒自动执行，每次新建实例
实例列表 triggerType=SCHEDULE，cost≈210ms，steps=3
连续触发 28 次后改为每 5 分钟；暂停 → next_fire_at 清空；启用 → 重算为 21:50:00
```

### 已知限制
- **错过补偿只实现 SKIP**：休眠/宕机后直接跳到下一个时间点，`miss_count` 有记录但不补跑；
  `FIRE_ONCE` / `CATCH_UP` 策略已预留字段与日志，尚未实现重放
- **单机调度**：多实例部署需要分布式锁（`SELECT ... FOR UPDATE SKIP LOCKED` 或 Redis 租约）
- `MQ` / `EVENT` 触发器只落库、未接消费端

---

## [1.3.1] - 2026-11-18

> 主题：**README 重写 —— 消除自相矛盾、同步到真实进度**

### 修正的矛盾
| 位置 | 原文 | 问题 | 现在 |
| --- | --- | --- | --- |
| 赞赏支持 | 「项目的 Roadmap 与代码**永久免费**」 | 与「内部项目、未开源」直接冲突 | 删除该表述，改为「与代码授权无关，不构成服务承诺」 |
| 许可 | 「内部项目，未开源发布」 + 徽章 `Internal` | 与「免费」并列时语义混乱 | 徽章改 `Proprietary`，明确「公开仓库 ≠ 开源，保留所有权利」 |
| 文档章节 | 「不随**开源仓库**分发」 | 自称开源仓库，与内部项目矛盾 | 改为「内部资料，仅保留在本地 docs/」 |

### 同步到真实进度
- 开场白从「已建成权限地基，正在向编排引擎推进」改为「主干链路已闭环」
- 能力章节重写：补齐领域模型（系统/连接器/接口/对外发布）、执行引擎、三方授权、网关限流与 IP 白名单
- 路线图补齐 v0.5–v1.3 全部已交付版本（原来有两个 v1.0，且缺 v1.1–v1.3），新增 v1.4 调度器 / v1.5 监控与死信
- 工程结构补充 `frontend/src/node-packages`（节点规格）、`assets/screens`、`docs/每日总结`
- 文档清单更新为当前份数（requirements 15 / design 16）
- 贡献约定新增「节点规格只改前端并运行 `pnpm sync:nodes`」

### 未改动
- 代码零改动，仅文档

---

## [1.3.0] - 2026-11-18

> 主题：**P0 收尾 + 端到端链路打通 + README 图文**

### 新增：对外网关限流与 IP 白名单
- 按接口维度 Redis 秒级计数，超过 `qps` 返回 409；实测 QPS=2 时第 3 次起被拒
- 调用方 IP 白名单（`t_app_client.allowed_ips`，支持逗号分隔与 `10.*` 前缀通配）
- IPv6 回环/映射地址归一化为 IPv4（`::1` → `127.0.0.1`、`::ffff:x` → `x`），
  否则本地调试会被自己的白名单挡住

### 新增：编排器三级联动选择
- HTTP 节点属性面板改为 **系统 → 接口 → 连接器** 三级下拉
- 选中接口自动带出契约（方法/路径/输出变量）与主连接器，不再手填编码
- 接口列表按系统过滤，连接器列表按接口已绑定的连接器过滤

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 设计器画布不刷新 | 点「新增」或切换流程时画布还是上一条 | `keep-alive` 按组件类型缓存，路由参数变化不重建 → `router-view` 用 `route.fullPath` 做 key |
| START 节点文字溢出 | 内容撑出节点框 | 节点尺寸固定 46px + 未裁剪 → 加 `overflow:hidden`，START 尺寸调为 150×58 |
| 编排接口鉴权不对 | 发布后的接口用签名调用返回「API Key 无效」 | 台账发布默认生成了 API_KEY 鉴权 → 改为默认 `SIGNATURE` |

### 端到端链路验证（调用方 → 平台 → 生产方 → API → 流程）
```
生产方 CRM 系统 + crm_main 连接器 + query-customer 接口   → /api/open/v1/crm-core/query-customer
平台编排接口 platform/order-detail（FLOW → demo-run-order）→ /api/open/v1/platform/order-detail
流程节点改为引用接口 crm-core/query-customer（不再写死连接器）
调用方换 JWT → 带签名调用 → 200，返回编排后的订单字段
```

### 文档
- README 增加 6 张界面截图（`assets/screens/`，压缩后共 623KB）
- README 增加「端到端链路」时序图，以及「接口 ↔ 流程」双向关系说明

---

## [1.2.0] - 2026-11-18

> 主题：**领域体系 + 接口多协议 + 接口关联流程 + API 资产页对齐原型**

### 新增：业务领域（`t_domain`）
- 集团口径初始化 11 个领域：财务 / 供应链 / 市场营销 / 销售 / 生产制造 / 人力资源 /
  客户服务 / 研发 / 主数据 / 信息技术 / 风控合规
- 系统归属领域；接口继承系统的领域，可按领域统计
- 领域管理页：新增/编辑/删除（领域下有系统时禁止删除）

### 新增：接口多协议
- `t_api.protocol`：HTTP / SOAP / JDBC / MQ / FILE
- 一个生产方系统可同时提供多种协议的接口（连接器各自声明协议）

### 新增：接口可关联流程编排
- `t_api.target_type`：`CONNECTOR`（直连连接器）/ `FLOW`（走流程）
- `t_api.flow_code`：绑定流程编码；发布时自动把对外发布类型设为 FLOW
- 流程节点也可直接引用接口：`config.apiCode = "systemCode/apiCode"`，
  执行器自动解析契约（方法/路径）与主连接器

### 新增：接口编号与 SLA
- `api_no` 自动生成：`API-{领域}-{4 位序号}`，如 `API-SCM-0001`
- `sla_level`：L1 核心 / L2 重要 / L3 一般

### 变更：API 资产页 1:1 对齐原型
- 页头：标题 + 纳管数量 + 批量校验 / 导出台账 / 注册接口
- 筛选条：搜索、业务域、SLA、状态 + 总数
- 主表格：接口（名称 + 编号·方法）、提供方、版本、SLA、QPS、状态、操作
- 右侧三块面板：**资产构成**（环形图 + 图例）、**规范校验**（通过率 + 3 条规则）、**待办**

### 校验增强
- 流程发布校验新增引用完整性：`FLOW_REF_API_MISSING` / `FLOW_REF_CONNECTOR_MISSING`

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| V15 迁移失败 | 应用启动失败 | `UPDATE` 中使用窗口函数，PG 不允许 |
| 接口编号错乱 | `API-GEN-5218` 之类 | 迁移先算编号后补领域，且用了 id 取模 → V16 重排为 `API-{领域}-{序号}` |
| 词条串块 | 页面显示 `ledger.colApi` 裸键 | 新词条被插入到 `user` 块（`fillRequired` 首个匹配位置） |

### 验收
```
领域     11 个已初始化（财务/供应链/…/风控合规）
系统     SYS_DEFAULT(IT) · erp-core(IT)
接口     API-IT-0001 order-query · API-IT-0002 order-orchestrated · API-IT-0003 query-order
引用校验 apiCode=erp-core/query-order 通过；not-exist 报 FLOW_REF_API_MISSING
```

---

## [1.1.0] - 2026-11-18

> 主题：**领域模型重构 —— 系统 / 连接器 / 接口 / 对外发布**
> 按「14-业务对象模型与关系」的结论落地四项决策：多连接器、环境维度、双版本控制、存量归入默认系统。

### 新增：生产方系统（`t_system`）
- 系统台账：编码、名称、**环境（DEV/TEST/PROD）**、主协议、默认地址、责任人
- 唯一键 `(tenant, code, env)`；删除时校验其下连接器与接口

### 新增：连接器归属系统
- `t_connection` 增 `system_code` / `protocol`，连接器从"孤立配置"变成"某系统的连接方式"
- 连接表单支持选择所属系统与协议（HTTP/SOAP/JDBC/MQ/FILE）

### 新增：接口台账（`t_api` + `t_api_version` + `t_api_connector`）
- **接口身份**与**契约版本**分离：身份稳定，契约可迭代
- 一个接口版本可绑定**多个连接器**（主/备，`role=MAIN/BACKUP`）
- 版本控制与流程一致：草稿 → 发布 → 旧版本 DEPRECATED

### 变更：对外路径自动生成
- 规则：`/api/open/v1/{systemCode}/{apiCode}`
- 发布接口时自动创建/更新对外发布记录，路径不可手填
- 实测：`/api/open/v1/erp-core/query-order` 直接可用（API Key 鉴权）

### 数据迁移（V14）
- 存量连接与接口统一归入默认系统 `SYS_DEFAULT`（PROD）
- 每条历史对外接口拆出「接口台账 + 1.0.0 契约版本 + 主连接器绑定」
- 迁移结果：`system 1 / api 2 / api_version 2 / api_connector 1`

### 前端
- 新增「系统注册」页（集成资产下）
- 「API 资产」改为接口台账：按系统筛选、契约与版本、绑定的连接器、对外地址、发布/下线

### 验收
```
系统台账      SYS_DEFAULT(连接器3/接口2) · erp-core PROD(连接器2/接口1)
接口台账      erp-core/query-order v1.0.0  GET /orders/...  conns=[erp_main, erp_backup]
对外地址      /api/open/v1/erp-core/query-order
实际调用      200，返回内部系统订单 JSON
```

### 已知未完成
- 编排器节点仍是 `connectionCode` 单级引用，未切到「系统+连接器+接口」三级
- START 节点文字溢出未修
- 老 `t_api_expose` 与新模型双写并存，下个版本收敛

---

## [1.0.0] - 2026-11-18

> 主题：**执行引擎 v0 —— 流程第一次真正跑起来**
> 这是分水岭版本：之前所有能力（DSL、编排器、连接器、开放接口）都是"能定义、能描述"，
> 从这一版开始**能执行**。

### 新增：执行引擎 v0

| 能力 | 说明 |
| --- | --- |
| 串行执行 | 沿 `next` / `CHOICE` 分支一路执行到 END，步数上限 `节点数×3+10` 防死循环 |
| 变量作用域 | `trigger`（触发入参）、`outputVar`（节点输出）、`nodes.<id>`、`sys.*`（traceId/now） |
| 节点执行器 | START / END / HTTP_INVOKE / TRANSFORM / CHOICE / DELAY / NOTIFY 七个 |
| 错误策略 | `FAIL` 中断、`SKIP` 跳过继续（读 manifest 的 `behavior.errorPolicy`） |
| 实例落库 | `t_flow_instance` + `t_flow_instance_node`，含每节点输入/输出/耗时/错误 |
| 调试接口 | `POST /flows/{id}/run` 同步执行并返回节点轨迹；`GET /instances`、`/instances/{id}` |

**表达式**：取值 `$resp.data.orderNo`、条件 `$order.amount > 100000`，
自研极简实现（取值 + 六种比较 + contains），不引入脚本引擎，安全边界可控。

### 新增：场景 A 打通（对外接口走流程）

`t_api_expose.target_type = FLOW` 时，`/api/open/**` 的请求不再透传，而是：

```
外部请求 → 平台执行流程（协议转换/字段映射/分支） → 返回流程输出
```

实测：外部 `GET /api/open/v1/order-detail` → 平台跑 5 个节点 → 返回映射后的订单字段。

### 新增：编排器「调试」真跑

- 调试按钮不再是提示语，而是真实执行一条实例
- 调试台输出逐节点轨迹：`✓ n2 HTTP_INVOKE 21ms`
- 失败节点在画布上标红

### 验收
```
试运行        SUCCESS 149ms
节点轨迹      n1 START 0ms / n2 HTTP_INVOKE 118ms / n3 TRANSFORM 0ms / n4 CHOICE 0ms / n5 END 0ms
流程输出      {"level":"VIP","amount":128000.5,"orderNo":"SO-20261118-001",...}
实例落库      4 条（MANUAL/HTTP 触发都有），节点轨迹可查
场景 A        外部调用 200，返回编排结果
```

---

## [0.9.1] - 2026-11-18

> 主题：**编排器编辑能力补齐（删除/复制/粘贴/右键菜单）**

### 新增
- **批量编辑**：框选或 Shift 多选后，`Delete` 批量删除、`⌘/Ctrl + C/V` 复制粘贴、`⌘/Ctrl + D` 创建副本、`⌘/Ctrl + A` 全选
- **右键菜单**：画布/node 上右键 → 复制 / 创建副本 / 粘贴 / 删除选中，带快捷键提示
- **复制语义**：连同选中节点之间的内部连线一起复制，粘贴自动重映射 ID 并偏移 40px，名称加「副本」后缀

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 连线删除按钮点不动 | 悬停出现但点击无反应 | 删除按钮是 SVG 内的 `<g>`，命中区域小且被连线自身的事件抢占 → 改为 HTML 按钮（22px、悬停放大、垃圾桶图标） |
| 删除图标不明确 | 一个手绘的 × | 换成 `DeleteOutlined` 图标 |

### 验收
```
连线悬停 -> 删除按钮出现 -> 点击   连线数 1 → 0
框选/右键 -> 删除选中              节点 2 → 1
Ctrl+C / Ctrl+V                    节点 +1（含内部连线重映射）
右键菜单                          复制 / 创建副本 / 粘贴 / 删除选中
```

---

## [0.9.0] - 2026-11-18

> 主题：**三方授权（B 注册接口 → A 授权调用）+ 交互修复**

### 新增：应用授权与签名调用

完整打通"内部接口对外提供"的三方模型：

```
B 系统：注册连接 + 登记接口
平台  ：创建应用（A）→ 发 appKey/appSecret → 授权接口
A 系统：签名换 JWT → 携带 JWT + MD5 签名调用 → 平台验签 → 转发 B
```

| 接口 | 说明 |
| --- | --- |
| `POST /api/open/auth/token` | `sign = MD5(appKey+timestamp+nonce+secret)`，返回 JWT（2h） |
| `GET/POST /api/open/**` | `Authorization: Bearer` + `X-Timestamp` / `X-Nonce` / `X-Sign` |
| `/api/v1/system/app-clients` | 应用 CRUD、重置密钥、接口授权 |

校验链：令牌有效 → 时间窗 ±5 分钟 → 签名一致 → nonce 未重放（Redis）→ 应用已授权该接口 →
转发上游 → 响应头 `X-EEIP-API` 标识接口编码。

**安全设计**：JWT 与用户令牌同密钥但 issuer 不同（`eeip-app`）互不通用；密钥只显示一次；
nonce 多实例共享 Redis 防重放。

### 新增：属性面板「输入 / 输出」
- 「输入」页签展示**上游节点**（自动推导）、**输入来源**（`$input` 或上游 `outputVar`）、输出变量
- 打通了"这个节点的数据从哪来、往哪去"，为执行引擎的变量作用域做准备

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 松手后节点仍跟随鼠标 | 拖动结束节点粘着光标 | 节点上的 `@pointerup.stop` 阻止冒泡，`window` 收不到 `pointerup`，拖拽状态不清空 |
| 连线无法选中 | 删除按钮只在悬停时出现 | 连线没有选中态 → 增加点击选中 + 选中后删除按钮常显 |

### 验收
```
① 换令牌         成功，有效期 7200s
② 带签名调用     200，X-EEIP-API = order-query，返回内部系统订单 JSON
③ 重放同一 nonce 401 nonce 已被使用（疑似重放）
④ 错误签名       401 签名校验失败
⑤ 拖动后松手     位置 (170,168) 保持；再移动鼠标不再跟随
⑥ 连线点击       删除按钮常显
⑦ 输入页签       上游节点 n1 · 定时入口；输入来源 $input
```

---

## [0.8.0] - 2026-11-18

> 主题：**接口开放（把内部接口封装后对外提供）+ 更多协议节点 + 拖拽落位修复**

### 新增：接口开放（API 资产管理）

产品定位升级：平台不只是"系统间的定时同步"，还是**各系统接口的管理与开放平台**。

| 能力 | 说明 |
| --- | --- |
| 接口登记 | `t_api_expose`：编码、方法、对外路径（支持 `{id}` 占位）、上游连接与路径模板、鉴权、QPS、版本 |
| 对外网关 | `/api/open/**` 免登录，按登记信息匹配 → 校验 API Key → 转发到上游 → 原样返回 |
| 鉴权 | `NONE` / `API_KEY`（`X-Api-Key`），Key 随机生成、只显示一次、可重置 |
| 管理界面 | API 资产页：列表、登记、编辑、重置 Key、**一键复制 curl** |
| 审计 | 登记/编辑/删除/重置 Key 全部进审计日志 |

实测：

```
无 key / 错误 key   -> 401 API Key 无效
正确 key            -> 200，响应头 X-EEIP-API: order-query
上游内容            {"code":0,"data":{"orderNo":"SO-20261118-001","customerLevel":"VIP",...}}
不存在的路径        -> 404
```

**设计要点**：转发目标是"管理员登记的连接"，属于受信配置，因此放行内网地址（`HttpSpec.trusted`）；
用户现填地址的连接测试仍走 SSRF 拦截 —— 两条路径不同风险等级，不能一刀切。

### 新增：更多协议与场景节点（17 种）

| 类别 | 新增 |
| --- | --- |
| 触发 | `MQ_SUBSCRIBE`（Topic/消费组/ACK/批量）、`SCHEDULE_CRON`（Cron/时区/错过策略）、`EVENT_ON`（事件名/过滤） |
| 调用 | `SOAP_INVOKE`（WSDL 操作）、`DB_QUERY`（SQL/最大行数）、`MQ_PUBLISH`（Topic/消息体）、`FILE_TRANSFER`（SFTP/FTP/S3/OSS） |
| 控制 | `PARALLEL`（并行分支）、`SUB_FLOW`（子流程） |

- 新增 `runtime.state`：`implemented` / `planned`
- **规划中的节点可以画进流程，但发布时会被拦住**：`FLOW_NODE_NOT_READY 执行器尚未实现`
  —— 避免"看起来能跑、实际没有执行器"

### 修复
- **拖动松手不落位**：拾取模式一旦开启（此前会持久化到 localStorage），再次进入时交互反转、
  松手节点仍在跟随。修复：① 拾取模式改为会话内生效、不记忆；② 拾取模式下若发生拖动，松开即落位；
  ③ 开启时顶部显示提示条

### 验收
```
拖拽前 (80,120) -> 拖动中 (224,200) -> 松手后 (224,200)  ✓ 落位
API 资产列表      显示 /api/open/orders/{id} · API_KEY · 100 QPS
规划中节点发布     FLOW_NODE_NOT_READY
新协议 schema      MQ_SUBSCRIBE 缺 topic -> CFG_REQUIRED
```

---

## [0.7.1] - 2026-11-18

> 主题：**编排器交互补齐 + 节点规格迁到前端**

### 新增
- **框选多选**：空白处拖拽拉出选框，命中节点整批选中，标题栏显示「已选 N 个节点」，可直接对齐/分布
- **拾取放置模式**：工具栏开关（记忆在 localStorage）。开启后点一下节点拿起、移动鼠标跟随、再点一下放下 ——
  与默认的"按住拖动"并存，适合触控板与像素级摆放
- 平移画布改为 `Alt/中键 + 拖拽`（把空白拖拽让给框选）

### 变更
- **节点规格迁到前端**：`frontend/src/node-packages/**/node.json` 成为唯一来源，
  前端 `import.meta.glob` 直接读取，不再请求 `/node-types`（后端不可用也能打开编排器）
- 新增 `scripts/sync-node-packages.mjs`：构建期把规格同步到后端资源做 config 校验，
  `--check` 模式用于 CI 防漂移
- 设计器内容与流程绑定：`/asset/flow/:id` 打开对应流程，路由参数变化时重新加载
- **新建流程为空白画布**：不再自动生成 START/END；首次保存时弹窗补填编码与名称

### 修复
- 同一组件实例内切换流程不刷新（`keep-alive` 缓存）→ 监听 `route.params.id` 重新加载
- 未知流程名时标题栏空白 → 显示「未命名流程」占位

### 验收
```
新建流程节点数        0（空白）
调色板添加            0 → 3
框选                 选中 3 个
左对齐后 X 坐标       [504, 504, 504]
拾取模式拿起          1 个节点跟随
切换流程              ops-alert-demo → 运维告警演示(4 节点) / bad-cycle → 有环的流程(4 节点)
```

---

## [0.7.0] - 2026-11-18

> 主题：**节点 Manifest 与注册中心（S8）**
> 把"节点定义"从三处硬编码（`FlowValidator` 常量表 / `FlowDsl` 注解 / 前端 `registry.ts`）
> 收敛为 **一个目录一个 `node.json`**，前后端都从它读。

### 新增

#### 节点规格（Manifest）
- `backend/src/main/resources/node-packages/*/node.json`：8 个内置节点规格
  （START / END / HTTP_INVOKE / TRANSFORM / CHOICE / DELAY / NOTIFY / SCRIPT）
- 规格含：`meta`（分组、多语言名、颜色、尺寸、排序、摘要模板）、`ports`、`config`（JSON Schema）、
  `behavior`（超时/重试/幂等/错误策略）、`runtime`（执行器定位）、`permissions`

#### 注册中心
- `NodeTypeRegistry`：启动扫描 `classpath*:node-packages/*/node.json`，
  按 `type → version` 双维度注册，取最高版本为默认；支持运行时注册扩展包
- `GET /api/v1/integration/node-types`：下发规格清单（前端渲染调色板与属性面板的唯一来源）
- `GET /api/v1/integration/node-types/{type}`：按类型取

#### 校验器改造
- `FlowValidator` 不再有 `NODE_TYPES` 常量与 `switch(type)` 分支，
  类型合法性查注册中心，配置校验由 manifest 的 JSON Schema 驱动
- 新增 `MiniSchemaValidator`：支持 `type / required / properties / additionalProperties /
  enum / minimum / maximum / pattern`，**配置写错 key 直接报 `CFG_UNKNOWN`**

#### 前端
- 调色板改为从后端拉取（`stores/nodeTypes.ts` + `api/nodeType.ts`），
  失败时回落内置规格，保证后端不可用也能打开编排器
- 属性面板字段由 manifest 的 schema 生成（`x-eeip.labelKey / widget / placeholder`）
- 节点按 `meta.order` 排序，分组顺序固定：触发 → 转换 → 路由 → 调用 → 控制

### 效果对比

| 场景 | 之前 | 现在 |
| --- | --- | --- |
| 新增节点类型 | 改 `FlowValidator` + `FlowDsl` 注解 + `registry.ts`，编译重启 | **加一个 `node-packages/新类型/node.json`** |
| 配置漏填 | 发布时才被节点专属校验发现 | `CFG_REQUIRED` 明确指出字段 |
| 配置拼错 key | 静默失效 | `CFG_UNKNOWN 未知配置项：mss` |
| 枚举非法 | 不校验 | `CFG_ENUM 取值必须是 [...] 之一` |
| 调色板 | 前端静态 8 类 + 5 个死按钮 | 后端下发，加节点即出现在面板 |

### 验收
```
启动日志      节点类型注册完成，共 8 种：[NOTIFY, START, CHOICE, SCRIPT, END, HTTP_INVOKE, DELAY, TRANSFORM]
GET /node-types   8 条，含 meta.order / ports / config
缺必填        CFG_REQUIRED 缺少必填配置：connectionCode（连接） [connectionCode]
拼错 key      CFG_UNKNOWN 未知配置项：mss [mss]
枚举非法      CFG_ENUM 配置项 channel 取值必须是 [DINGTALK, WECOM, EMAIL, WEBHOOK] 之一
前端          请求 node-types 1 次，调色板 8 项、5 组、按 order 排列
```
截图：`docs/screenshots/s8-palette.png`（本地）

---

## [0.6.2] - 2026-11-18

> 主题：**编排器重构 —— 通用节点规格 + 组件封装 + 交互修正**

### 背景
第一版编排器存在四个硬伤：调色板多数条目点了没反应、拖动节点后节点会一直跟着鼠标、
连线不好操作、无法对齐。本轮把编排器拆成可复用组件，并把节点定义抽象成规格表。

### 新增
- **节点规格注册表** `components/flow/registry.ts`：一个节点 = 一条规格
  （分组、颜色、尺寸、端口、默认名、默认配置、属性字段、画布摘要）。
  调色板、画布、属性面板全部由规格驱动，**加节点类型不需要改组件**。
- 新节点类型：`DELAY`、`NOTIFY`、`SCRIPT`（后端校验同步支持，含专属配置检查）
- **对齐工具**：左/水平居中/右/顶/垂直居中/底 + 水平/垂直分布，
  `Shift + 点击`多选后生效
- 调色板支持**拖拽到画布**（落点即节点位置），未实现的类型显示「后续版本支持」标签并可点击（给提示，不再是死按钮）

### 重构
| 之前 | 现在 |
| --- | --- |
| `Designer.vue` 722 行，逻辑全在一起 | `registry.ts`（规格）+ `useFlowModel`（状态与对齐）+ `FlowCanvas.vue`（渲染交互）+ `Designer.vue`（组装） |
| 节点尺寸/配色硬编码在模板里 | 由 `spec.size` / `spec.color` 驱动 |
| 属性面板一堆 v-if 判断类型 | 按 `spec.fields` 循环渲染 |

### 修复
| 缺陷 | 现象 | 根因 |
| --- | --- | --- |
| 节点跟随鼠标 | 松手后节点仍粘着光标 | `pointerup` 只绑在画布上，在属性面板或窗口外松手不会结束拖拽 → 改为 window 级监听 |
| 连线难操作 | 必须精确拖到节点上 | 端口命中区 9px + 点击式连线（点端口→点目标）+ 连线悬停显示删除按钮 |
| 调色板死条目 | 点了没有任何反馈 | 未实现类型现在也有响应（提示），已实现类型可点可拖 |

### 关于是否用开源画布
评估过 Vue Flow / LogicFlow / X6：它们面向"自由拓扑图"，而本产品 DSL 是
**带 CHOICE 分支的线性流程**（`next` + `branches`），且需要严格对齐暗色设计令牌。
接入开源库要做二次映射与样式覆盖，成本高于自研；因此自研但**封装成独立组件**，
后续若要换成 Vue Flow，只需替换 `FlowCanvas.vue`，模型层不动。

### 验收
```
调色板加 4 类节点        2 → 6
拖拽节点后松开          位移 160px，继续移动鼠标漂移 0px
点击端口再点目标节点     生成连线 1 条
多选 2 个节点 + 左对齐   工具条显示「已选 2 个节点」
新类型校验               DELAY/NOTIFY/SCRIPT 通过
缺配置                   FLOW_DELAY_MS 需要配置 ms
```
截图：`docs/screenshots/s7d-designer.png`、`s7d-align.png`（本地）

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
