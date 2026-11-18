/**
 * 繁體中文
 */
export default {
  common: {
    query: '查詢', refresh: '重新整理', create: '新增', edit: '編輯', delete: '刪除',
    save: '儲存', cancel: '取消', confirm: '確定', test: '測試', testing: '測試中',
    actions: '操作', status: '狀態', enabled: '啟用', disabled: '停用', remark: '備註',
    copy: '複製', copied: '已複製', total: '共 {n} 筆', pageSize: '筆/頁',
    yes: '是', no: '否', all: '全部', back: '返回', more: '更多',
    success: '成功', failed: '失敗', none: '無', unset: '未設定', untested: '未測試',
    createdAt: '建立時間', updatedAt: '更新時間', keywords: '關鍵字',
    functionOnlyDev: '此模組將於後續迭代實作', planned: '計畫內容：'
  },
  app: {
    name: 'EEIP', subtitle: '企業級系統整合平台', env: 'prod · 主叢集',
    logout: '登出', language: '語言', searchPlaceholder: '搜尋介面 / 流程 / 系統'
  },
  login: {
    title: '登入主控台', subtitle: '使用企業帳號登入。首次登入請聯繫管理員指派角色。',
    username: '使用者名稱', password: '密碼', tenant: '租戶代碼', submit: '進入主控台',
    headline1: '讓每一次系統對話', headline2: '都有據可循',
    intro: '統一接入、視覺化編排、全鏈路可觀測。把散落在各系統之間的直連，收斂為一條可治理、可追溯的整合底座。',
    metricSystems: '個系統', metricSystemsSub: '統一納管',
    metricTps: 'TPS', metricTpsSub: '設計吞吐',
    metricSla: '%', metricSlaSub: '平台可用性',
    metricAudit: '%', metricAuditSub: '呼叫可稽核',
    tipTitle: '內建帳號（開發環境）：',
    tipAdmin: '集團超級管理員',
    tipDept: '部門管理員（僅看本部門）',
    tipPartner: '合作方租戶（資料完全隔離）',
    required: '請輸入使用者名稱與密碼', success: '登入成功'
  },
  dashboard: {
    title: '概覽', welcome: '歡迎回來，{name}。目前身分：{roles}',
    menuCount: '我可存取的選單', permCount: '我持有的權限點', availability: '平台可用性（示例）',
    throughput: '即時吞吐（示例）', unitItem: '項', unitPerm: '個',
    noticeTitle: '本頁用於驗證 RBAC 登入閉環',
    noticeBody: '左側選單由後端 /api/v1/auth/me 回傳的權限樹渲染；不同帳號登入會看到不同選單。系統管理下的使用者/角色/租戶/連線頁面已接入真實介面與按鈕級權限。'
  },
  user: {
    title: '使用者管理', desc: '帳號、角色與狀態維護。按鈕會依目前登入者的權限點自動顯示或隱藏。',
    searchPlaceholder: '使用者名稱 / 姓名 / 部門',
    colUsername: '使用者名稱', colNickname: '姓名', colDept: '部門', colRoles: '角色',
    colLastLogin: '最近登入',
    create: '新增使用者', editTitle: '編輯使用者 · {name}', createTitle: '新增使用者',
    passwordTip: '密碼（留空則不修改）', password: '初始密碼', passwordPlaceholder: '至少 8 位',
    unassigned: '未指派', resetPwd: '重設密碼', resetTitle: '重設密碼 · {name}',
    newPassword: '新密碼', deleteConfirm: '確認刪除使用者「{name}」？',
    deleteHint: '刪除後該帳號將無法登入，操作會記錄至稽核日誌。',
    fillRequired: '請填寫姓名與初始密碼', saved: '已儲存', created: '已建立', deleted: '已刪除',
    pwdTooShort: '密碼至少 8 位', pwdReset: '密碼已重設'
  },
  role: {
    title: '角色管理',
    desc: '角色決定選單可見性與按鈕權限；修改權限後，持有該角色的使用者會在 30 秒內（快取失效）重新取得並生效。',
    searchPlaceholder: '角色名稱',
    colName: '角色名稱', colCode: '代碼', colScope: '資料範圍', colMembers: '成員數',
    colSort: '排序', colType: '類型', colRemark: '備註',
    create: '新增角色', createTitle: '新增角色', editTitle: '編輯角色 · {name}',
    assign: '指派權限', assignTitle: '指派權限 · {name}',
    scopeAll: '全部資料', scopeTenant: '本租戶', scopeProject: '本專案（同部門）', scopeSelf: '僅本人',
    builtin: '內建', custom: '自訂', deleteConfirm: '確認刪除角色「{name}」？',
    codePlaceholder: '如 ORDER_ADMIN', fillRequired: '請填寫角色代碼與名稱',
    saved: '已儲存', created: '已建立', deleted: '已刪除',
    assignHint: '勾選目錄/選單決定側邊欄可見範圍，勾選按鈕決定頁面內的操作權限。',
    assignSuccess: '權限已更新，相關使用者下次請求即生效'
  },
  tenant: {
    title: '租戶管理',
    desc: '租戶之間資料完全隔離：登入時依租戶代碼識別，之後所有 SQL 自動帶上 tenant_id 條件。平台級表（租戶、權限點）不參與過濾。',
    colCode: '租戶代碼', colName: '租戶名稱', colType: '類型', colContact: '聯絡人',
    searchPlaceholder: '代碼 / 名稱',
    colPhone: '聯絡電話',
    internal: '內部', partner: '合作方',
    provision: '一鍵開通租戶', createOnly: '僅建租戶', createTitle: '新增租戶', editTitle: '編輯租戶 · {name}',
    codePlaceholder: '如 SUPPLIER_D', namePlaceholder: '如 供應商D',
    provisionHint: '會自動建立：租戶 → 兩個內建角色（租戶管理員 / 租戶開發）→ 管理員帳號 → 授權',
    adminUser: '管理員使用者名稱', adminName: '管理員姓名', adminNamePlaceholder: '留空自動產生',
    provisionOk: '開通並產生管理員', provisionDone: '租戶已開通',
    pwdWarn: '初始密碼只顯示這一次，請立即保存並交付給對方',
    granted: '已授權權限點', saveCred: '複製憑證', savedIt: '我已保存',
    createHint: '新建租戶後，需要在庫中為該租戶初始化角色與管理員帳號（建議使用「一鍵開通」）',
    deleteConfirm: '確認刪除租戶「{name}」？',
    deleteHint: '刪除後該租戶帳號將無法登入，歷史資料仍保留於庫中。',
    fillRequired: '請填寫租戶代碼與名稱', saved: '已儲存', created: '已建立', deleted: '已刪除'
  },
  conn: {
    title: '連線管理',
    desc: '外部系統的連線設定與連通性測試。憑證只進不出：儲存後任何介面都不回傳原始憑證。對外請求預設禁止存取內網位址（SSRF 防護）。',
    searchPlaceholder: '代碼 / 名稱',
    colCode: '代碼', colName: '名稱', colEndpoint: '位址', colMethod: '方法',
    colAuth: '認證', colLastCheck: '最近測試',
    create: '新增連線', createTitle: '新增連線', editTitle: '編輯連線 · {name}',
    codePlaceholder: '小寫字母開頭，如 github_zen', namePlaceholder: '如 GitHub 公開 API',
    endpointPlaceholder: 'https://api.github.com/zen',
    authConfig: '認證設定', authConfigEdit: '認證設定（留空表示不修改）',
    authPlaceholder: 'BEARER: {"token":"xxx"} / BASIC: {"username":"u","password":"p"}',
    headers: '請求標頭 JSON', timeout: '逾時（毫秒）',
    precheck: '儲存前預檢', precheckTitle: '預檢結果（未儲存）',
    testTitle: '連通性測試 · {name}',
    result: '結果', statusCode: '狀態碼', cost: '耗時', body: '回應內容（截斷至 500 字元）',
    fillRequired: '請填寫代碼與位址', endpointRequired: '請先填寫位址',
    deleteConfirm: '確認刪除連線「{name}」？',
    saved: '已儲存', created: '已建立', deleted: '已刪除'
  },
  audit: {
    title: '稽核日誌',
    desc: '所有寫入操作（使用者 / 角色 / 租戶 / 連線 / 權限）都會落一筆稽核記錄，包含操作人、參數（已脫敏）、結果與耗時。稽核只追加，不提供修改與刪除介面。',
    operatorPlaceholder: '操作人（工號/姓名）', actionAll: '全部動作', resultAll: '全部結果',
    colTime: '時間', colOperator: '操作人', colAction: '動作', colResource: '對象',
    colResult: '結果', colCost: '耗時', colIp: 'IP',
    request: '請求', params: '參數（脫敏）', errorMsg: '失敗原因'
  },
  flow: {
    title: '流程編排',
    desc: '流程定義、DSL 校驗與版本發佈。發佈前會做結構校驗、引用完整性、可達性與環檢測，任何一項不過都不允許上線。',
    searchPlaceholder: '代碼 / 名稱',
    colCode: '代碼', colName: '名稱', colStatus: '狀態', colVersion: '版本',
    colNodes: '節點數', colHash: 'DSL 摘要',
    create: '新增流程', createTitle: '新增流程', editTitle: '編輯流程 · {name}',
    triggerType: '觸發方式', changeLog: '變更說明', changeLogPlaceholder: '本次改了什麼',
    dsl: '流程 DSL', format: '格式化', validate: '校驗',
    preview: '流程預覽（唯讀）', previewEmpty: 'DSL 解析失敗，無法預覽',
    validatePass: '校驗通過，可以發佈', validateFail: '校驗未通過，請修正後重試',
    publish: '發佈', offline: '下線',
    publishConfirm: '確認發佈流程「{name}」？',
    publishHint: '發佈前會自動校驗；通過後產生新版本，舊版本設為 DEPRECATED。',
    published: '已發佈 v{v}', offlined: '已下線',
    deleteConfirm: '確認刪除流程「{name}」？',
    statusDRAFT: '草稿', statusPUBLISHED: '已發佈', statusOFFLINE: '已下線',
    jsonInvalid: 'JSON 格式錯誤', saved: '已儲存', created: '已建立', deleted: '已刪除'
  },

  placeholder: {
    apiTitle: 'API 資產', apiDesc: '介面清冊、規格校驗與生命週期管理',
    flowTitle: '流程編排', flowDesc: '視覺化拖曳編排與線上除錯',
    consoleTitle: '整合作戰室', consoleDesc: '即時資料流向、告警與容量水位',
    dlqTitle: '死信處置', dlqDesc: '失敗訊息歸因、重投與留痕',
    auditTitle: '稽核日誌', auditDesc: '操作稽核與資料存取稽核'
  },
  err: {
    notFound: '頁面不存在或已被移除', backHome: '返回首頁'
  }
}
