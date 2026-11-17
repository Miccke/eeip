/**
 * English
 */
export default {
  common: {
    query: 'Search', refresh: 'Refresh', create: 'Create', edit: 'Edit', delete: 'Delete',
    save: 'Save', cancel: 'Cancel', confirm: 'OK', test: 'Test', testing: 'Testing',
    actions: 'Actions', status: 'Status', enabled: 'Enabled', disabled: 'Disabled', remark: 'Remark',
    copy: 'Copy', copied: 'Copied', total: 'Total {n}', pageSize: '/ page',
    yes: 'Yes', no: 'No', all: 'All', back: 'Back', more: 'More',
    success: 'Success', failed: 'Failed', none: 'None', unset: 'Not set', untested: 'Not tested',
    createdAt: 'Created', updatedAt: 'Updated', keywords: 'Keywords',
    functionOnlyDev: 'This module will be implemented in a later iteration', planned: 'Planned:'
  },
  app: {
    name: 'EEIP', subtitle: 'Enterprise Integration Platform', env: 'prod · primary',
    logout: 'Sign out', language: 'Language', searchPlaceholder: 'Search APIs / flows / systems'
  },
  login: {
    title: 'Sign in', subtitle: 'Use your corporate account. Contact your admin for role assignment.',
    username: 'Username', password: 'Password', tenant: 'Tenant code', submit: 'Enter console',
    headline1: 'Every system conversation', headline2: 'stays traceable',
    intro: 'Unified access, visual orchestration, end-to-end observability. Turn scattered point-to-point links into one governed, traceable integration foundation.',
    metricSystems: 'systems', metricSystemsSub: 'Managed',
    metricTps: 'TPS', metricTpsSub: 'Design throughput',
    metricSla: '%', metricSlaSub: 'Availability',
    metricAudit: '%', metricAuditSub: 'Audited calls',
    tipTitle: 'Demo accounts:',
    tipAdmin: 'platform super admin',
    tipDept: 'department admin (own dept only)',
    tipPartner: 'partner tenant (fully isolated)',
    required: 'Please enter username and password', success: 'Signed in'
  },
  dashboard: {
    title: 'Overview', welcome: 'Welcome back, {name}. Roles: {roles}',
    menuCount: 'Menus available', permCount: 'Permissions held', availability: 'Availability (demo)',
    throughput: 'Live throughput (demo)', unitItem: '', unitPerm: '',
    noticeTitle: 'This page verifies the RBAC sign-in loop',
    noticeBody: 'The sidebar renders the permission tree returned by /api/v1/auth/me; different accounts see different menus. Users / Roles / Tenants / Connections / Audit pages are wired to real APIs with button-level permissions.'
  },
  user: {
    title: 'Users', desc: 'Accounts, roles and status. Buttons appear or hide according to the current user\'s permissions.',
    searchPlaceholder: 'Username / name / department',
    colUsername: 'Username', colNickname: 'Name', colDept: 'Department', colRoles: 'Roles',
    colLastLogin: 'Last login',
    create: 'New user', editTitle: 'Edit user · {name}', createTitle: 'New user',
    passwordTip: 'Password (leave blank to keep)', password: 'Initial password', passwordPlaceholder: 'At least 8 characters',
    unassigned: 'Unassigned', resetPwd: 'Reset password', resetTitle: 'Reset password · {name}',
    newPassword: 'New password', deleteConfirm: 'Delete user "{name}"?',
    deleteHint: 'The account will no longer be able to sign in. This action is audited.',
    fillRequired: 'Please fill in name and initial password', saved: 'Saved', created: 'Created', deleted: 'Deleted',
    pwdTooShort: 'Password must be at least 8 characters', pwdReset: 'Password reset'
  },
  role: {
    title: 'Roles',
    desc: 'Roles define menu visibility and button permissions. After changes, affected users pick them up on their next request (cache invalidated within 30s).',
    searchPlaceholder: 'Role name',
    colName: 'Role', colCode: 'Code', colScope: 'Data scope', colMembers: 'Members',
    colSort: 'Order', colType: 'Type', colRemark: 'Remark',
    create: 'New role', createTitle: 'New role', editTitle: 'Edit role · {name}',
    assign: 'Assign permissions', assignTitle: 'Assign permissions · {name}',
    scopeAll: 'All data', scopeTenant: 'Tenant', scopeProject: 'Department', scopeSelf: 'Own records only',
    builtin: 'Built-in', custom: 'Custom', deleteConfirm: 'Delete role "{name}"?',
    codePlaceholder: 'e.g. ORDER_ADMIN', fillRequired: 'Please fill in role code and name',
    saved: 'Saved', created: 'Created', deleted: 'Deleted',
    assignHint: 'Checked menus control the sidebar; checked buttons control in-page actions.',
    assignSuccess: 'Permissions updated, effective on next request'
  },
  tenant: {
    title: 'Tenants',
    desc: 'Tenants are fully isolated: the tenant is identified at sign-in, and every subsequent SQL carries tenant_id automatically. Platform tables (tenants, permissions) are excluded.',
    colCode: 'Code', colName: 'Name', colType: 'Type', colContact: 'Contact', colPhone: 'Phone',
    searchPlaceholder: 'Code / name',
    internal: 'Internal', partner: 'Partner',
    provision: 'Provision tenant', createOnly: 'Record only', createTitle: 'New tenant', editTitle: 'Edit tenant · {name}',
    codePlaceholder: 'e.g. SUPPLIER_D', namePlaceholder: 'e.g. Supplier D',
    provisionHint: 'Creates: tenant → two built-in roles (tenant admin / developer) → admin account → permissions',
    adminUser: 'Admin username', adminName: 'Admin name', adminNamePlaceholder: 'Auto-generated if blank',
    provisionOk: 'Provision and create admin', provisionDone: 'Tenant provisioned',
    pwdWarn: 'The initial password is shown only once. Save it and hand it over now.',
    granted: 'Permissions granted', saveCred: 'Copy credentials', savedIt: 'I have saved it',
    createHint: 'After creating a tenant record, roles and an admin must be initialized (use Provision instead).',
    deleteConfirm: 'Delete tenant "{name}"?',
    deleteHint: 'Accounts in this tenant will not be able to sign in; historical data is retained.',
    fillRequired: 'Please fill in tenant code and name', saved: 'Saved', created: 'Created', deleted: 'Deleted'
  },
  conn: {
    title: 'Connections',
    desc: 'Connection settings and connectivity tests. Credentials are write-only and never returned. Outbound calls to private networks are blocked by default (SSRF protection).',
    searchPlaceholder: 'Code / name',
    colCode: 'Code', colName: 'Name', colEndpoint: 'Endpoint', colMethod: 'Method',
    colAuth: 'Auth', colLastCheck: 'Last test',
    create: 'New connection', createTitle: 'New connection', editTitle: 'Edit connection · {name}',
    codePlaceholder: 'lowercase, e.g. github_zen', namePlaceholder: 'e.g. GitHub public API',
    endpointPlaceholder: 'https://api.github.com/zen',
    authConfig: 'Auth config', authConfigEdit: 'Auth config (leave blank to keep)',
    authPlaceholder: 'BEARER: {"token":"xxx"} / BASIC: {"username":"u","password":"p"}',
    headers: 'Header JSON', timeout: 'Timeout (ms)',
    precheck: 'Pre-check', precheckTitle: 'Pre-check result (not saved)',
    testTitle: 'Connectivity test · {name}',
    result: 'Result', statusCode: 'Status', cost: 'Latency', body: 'Response body (truncated to 500 chars)',
    fillRequired: 'Please fill in code and endpoint', endpointRequired: 'Please fill in the endpoint first',
    deleteConfirm: 'Delete connection "{name}"?',
    saved: 'Saved', created: 'Created', deleted: 'Deleted'
  },
  audit: {
    title: 'Audit logs',
    desc: 'Every write operation (users / roles / tenants / connections / permissions) produces an audit record with operator, masked parameters, result and latency. Audit logs are append-only.',
    operatorPlaceholder: 'Operator (id or name)', actionAll: 'All actions', resultAll: 'All results',
    colTime: 'Time', colOperator: 'Operator', colAction: 'Action', colResource: 'Object',
    colResult: 'Result', colCost: 'Latency', colIp: 'IP',
    request: 'Request', params: 'Parameters (masked)', errorMsg: 'Reason'
  },
  placeholder: {
    apiTitle: 'API Assets', apiDesc: 'API inventory, spec validation and lifecycle',
    flowTitle: 'Flow Designer', flowDesc: 'Visual orchestration and online debugging',
    consoleTitle: 'Command Center', consoleDesc: 'Live data flow, alerts and capacity',
    dlqTitle: 'Dead Letters', dlqDesc: 'Failure triage, replay and audit trail',
    auditTitle: 'Audit Logs', auditDesc: 'Operation and data-access auditing'
  },
  err: {
    notFound: 'Page not found', backHome: 'Back to home'
  }
}
