/**
 * 简体中文
 */
export default {
  common: {
    query: '查询', refresh: '刷新', create: '新增', edit: '编辑', delete: '删除',
    save: '保存', cancel: '取消', confirm: '确定', test: '测试', testing: '测试中',
    actions: '操作', status: '状态', enabled: '启用', disabled: '停用', remark: '备注',
    copy: '复制', copied: '已复制', total: '共 {n} 条', pageSize: '条/页',
    yes: '是', no: '否', all: '全部', back: '返回', more: '更多',
    success: '成功', failed: '失败', none: '无', unset: '未设置', untested: '未测试',
    createdAt: '创建时间', updatedAt: '更新时间', keywords: '关键字',
    functionOnlyDev: '该模块将在后续迭代实现', planned: '计划内容：'
  },
  app: {
    name: 'EEIP', subtitle: '企业级系统集成平台', env: 'prod · 主集群',
    logout: '退出登录', language: '语言', searchPlaceholder: '搜索接口 / 流程 / 系统'
  },
  login: {
    title: '登录控制台', subtitle: '使用企业账号登录。首次登录请联系管理员分配角色。',
    username: '用户名', password: '密码', tenant: '租户编码', submit: '进入控制台',
    headline1: '让每一次系统对话', headline2: '都有据可循',
    intro: '统一接入、可视化编排、全链路可观测。把散落在各系统之间的直连，收敛为一条可治理、可追溯的集成底座。',
    metricSystems: '个系统', metricSystemsSub: '统一纳管',
    metricTps: 'TPS', metricTpsSub: '设计吞吐',
    metricSla: '%', metricSlaSub: '平台可用性',
    metricAudit: '%', metricAuditSub: '调用可审计',
    tipTitle: '内置账号（开发环境）：',
    tipAdmin: '集团超管',
    tipDept: '部门管理员（只看本部门）',
    tipPartner: '合作方租户（数据完全隔离）',
    required: '请输入用户名与密码', success: '登录成功'
  },
  dashboard: {
    title: '概览', welcome: '欢迎回来，{name}。当前身份：{roles}',
    menuCount: '我可访问的菜单', permCount: '我持有的权限点', availability: '平台可用性（示例）',
    throughput: '实时吞吐（示例）', unitItem: '项', unitPerm: '个',
    noticeTitle: '本页用于验证 RBAC 登录闭环',
    noticeBody: '左侧菜单由后端 /api/v1/auth/me 返回的权限树渲染；不同账号登录会看到不同菜单。系统管理下的用户/角色/租户/连接页面已接入真实接口与按钮级权限。'
  },
  user: {
    title: '用户管理', desc: '账号、角色与状态维护。按钮会根据当前登录人的权限点自动显示或隐藏。',
    searchPlaceholder: '用户名 / 姓名 / 部门',
    colUsername: '用户名', colNickname: '姓名', colDept: '部门', colRoles: '角色',
    colLastLogin: '最近登录',
    create: '新增用户', editTitle: '编辑用户 · {name}', createTitle: '新增用户',
    passwordTip: '密码（留空则不修改）', password: '初始密码', passwordPlaceholder: '至少 8 位',
    unassigned: '未分配', resetPwd: '重置密码', resetTitle: '重置密码 · {name}',
    newPassword: '新密码', deleteConfirm: '确认删除用户「{name}」？',
    deleteHint: '删除后该账号将无法登录，操作会记录到审计日志。',
    fillRequired: '请填写姓名与初始密码', saved: '已保存', created: '已创建', deleted: '已删除',
    pwdTooShort: '密码至少 8 位', pwdReset: '密码已重置'
  },
  role: {
    title: '角色管理',
    desc: '角色决定菜单可见性与按钮权限；修改权限后，持有该角色的用户在 30 秒内（缓存失效）重新拉取即可生效。',
    searchPlaceholder: '角色名称',
    colName: '角色名称', colCode: '编码', colScope: '数据范围', colMembers: '成员数',
    colSort: '排序', colType: '类型', colRemark: '备注',
    create: '新增角色', createTitle: '新增角色', editTitle: '编辑角色 · {name}',
    assign: '分配权限', assignTitle: '分配权限 · {name}',
    scopeAll: '全部数据', scopeTenant: '本租户', scopeProject: '本项目（同部门）', scopeSelf: '仅本人',
    builtin: '内置', custom: '自定义', deleteConfirm: '确认删除角色「{name}」？',
    codePlaceholder: '如 ORDER_ADMIN', fillRequired: '请填写角色编码与名称',
    saved: '已保存', created: '已创建', deleted: '已删除',
    assignHint: '勾选目录/菜单决定侧边栏可见范围，勾选按钮决定页面内的操作权限。',
    assignSuccess: '权限已更新，相关用户下次请求即生效'
  },
  tenant: {
    title: '租户管理',
    desc: '租户之间数据完全隔离：登录时按租户编码识别，之后所有 SQL 自动带上 tenant_id 条件。平台级表（租户、权限点）不参与过滤。',
    colCode: '租户编码', colName: '租户名称', colType: '类型', colContact: '联系人',
    searchPlaceholder: '编码 / 名称',
    colPhone: '联系电话',
    internal: '内部', partner: '合作方',
    provision: '一键开通租户', createOnly: '仅建租户', createTitle: '新增租户', editTitle: '编辑租户 · {name}',
    codePlaceholder: '如 SUPPLIER_D', namePlaceholder: '如 供应商D',
    provisionHint: '会自动创建：租户 → 两个内置角色（租户管理员 / 租户开发）→ 管理员账号 → 授权',
    adminUser: '管理员用户名', adminName: '管理员姓名', adminNamePlaceholder: '留空自动生成',
    provisionOk: '开通并生成管理员', provisionDone: '租户已开通',
    pwdWarn: '初始密码只显示这一次，请立即保存并交付给对方',
    granted: '已授权权限点', saveCred: '复制凭据', savedIt: '我已保存',
    createHint: '新建租户后，需要在库中为该租户初始化角色与管理员账号（推荐使用「一键开通」）',
    deleteConfirm: '确认删除租户「{name}」？',
    deleteHint: '删除后该租户账号将无法登录，历史数据仍保留在库中。',
    fillRequired: '请填写租户编码与名称', saved: '已保存', created: '已创建', deleted: '已删除'
  },
  conn: {
    title: '连接管理',
    desc: '外部系统的连接配置与连通性测试。凭证只进不出：保存后任何接口都不返回原始凭证。出站请求默认禁止访问内网地址（SSRF 防护）。',
    searchPlaceholder: '编码 / 名称',
    colCode: '编码', colName: '名称', colEndpoint: '地址', colMethod: '方法',
    colAuth: '认证', colLastCheck: '最近测试',
    create: '新建连接', createTitle: '新建连接', editTitle: '编辑连接 · {name}',
    codePlaceholder: '小写字母开头，如 github_zen', namePlaceholder: '如 GitHub 公开 API',
    endpointPlaceholder: 'https://api.github.com/zen',
    authConfig: '认证配置', authConfigEdit: '认证配置（留空表示不修改）',
    authPlaceholder: 'BEARER: {"token":"xxx"} / BASIC: {"username":"u","password":"p"}',
    headers: '请求头 JSON', timeout: '超时（毫秒）',
    precheck: '保存前预检', precheckTitle: '预检结果（未保存）',
    testTitle: '连通性测试 · {name}',
    result: '结果', statusCode: '状态码', cost: '耗时', body: '响应体（截断至 500 字符）',
    fillRequired: '请填写编码与地址', endpointRequired: '请先填写地址',
    deleteConfirm: '确认删除连接「{name}」？',
    saved: '已保存', created: '已创建', deleted: '已删除'
  },
  audit: {
    title: '审计日志',
    desc: '所有写操作（用户 / 角色 / 租户 / 连接 / 权限）都会落一条审计记录，包含操作人、参数（已脱敏）、结果与耗时。审计只追加，不提供修改与删除接口。',
    operatorPlaceholder: '操作人（工号/姓名）', actionAll: '全部动作', resultAll: '全部结果',
    colTime: '时间', colOperator: '操作人', colAction: '动作', colResource: '对象',
    colResult: '结果', colCost: '耗时', colIp: 'IP',
    request: '请求', params: '参数（脱敏）', errorMsg: '失败原因'
  },
  flow: {
    title: '流程编排',
    desc: '流程定义、DSL 校验与版本发布。发布前会做结构校验、引用完整性、可达性与环检测，任何一项不过都不允许上线。',
    searchPlaceholder: '编码 / 名称',
    colCode: '编码', colName: '名称', colStatus: '状态', colVersion: '版本',
    colNodes: '节点数', colHash: 'DSL 摘要',
    create: '新建流程', createTitle: '新建流程', editTitle: '编辑流程 · {name}',
    triggerType: '触发方式', changeLog: '变更说明', changeLogPlaceholder: '本次改了什么',
    dsl: '流程 DSL', format: '格式化', validate: '校验',
    preview: '流程预览（只读）', previewEmpty: 'DSL 解析失败，无法预览',
    validatePass: '校验通过，可以发布', validateFail: '校验未通过，请修正后重试',
    publish: '发布', offline: '下线',
    publishConfirm: '确认发布流程「{name}」？',
    publishHint: '发布前会自动校验；通过后生成新版本，旧版本置为 DEPRECATED。',
    published: '已发布 v{v}', offlined: '已下线',
    deleteConfirm: '确认删除流程「{name}」？',
    statusDRAFT: '草稿', statusPUBLISHED: '已发布', statusOFFLINE: '已下线',
    jsonInvalid: 'JSON 格式错误', saved: '已保存', created: '已创建', deleted: '已删除'
  },

  placeholder: {
    apiTitle: 'API 资产', apiDesc: '接口台账、规范校验与生命周期管理',
    flowTitle: '流程编排', flowDesc: '可视化拖拽编排与在线调试',
    consoleTitle: '集成作战室', consoleDesc: '实时数据流向、告警与容量水位',
    dlqTitle: '死信处置', dlqDesc: '失败消息归因、重投与留痕',
    auditTitle: '审计日志', auditDesc: '操作审计与数据访问审计'
  },
  err: {
    notFound: '页面不存在或已被移除', backHome: '返回首页'
  }
}
