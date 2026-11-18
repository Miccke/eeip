/**
 * 节点规格注册表 —— 编排器的唯一扩展点。
 *
 * 新增一种节点只需在这里加一条：调色板、画布配色与尺寸、属性面板字段、
 * 默认配置、连线路由全部由规格驱动，不需要改组件。
 * 后端同步：FlowValidator.NODE_TYPES + 类型专属校验。
 */
import type { FlowNode } from '@/api/flow'

export type NodeType = string

/** 调色板分组 */
export type GroupKey = 'trigger' | 'transform' | 'route' | 'invoke' | 'control'

/** 属性面板字段类型 */
export type FieldKind = 'text' | 'number' | 'select' | 'textarea'

export interface FieldSpec {
  key: string
  /** manifest 里的 title（已本地化或原文） */
  label?: string
  labelKey: string
  kind: FieldKind
  placeholder?: string
  options?: { label: string; value: string }[]
  min?: number
  max?: number
  /** 值为空时的默认值，渲染时自动补 */
  fallback?: string | number
}

export interface NodeSpec {
  type: NodeType
  group: GroupKey
  /** 已按当前语言解析好的显示名（来自 manifest）；内置规格用 labelKey 兜底 */
  label?: string
  /** 调色板与属性面板标题的 i18n key（内置规格使用） */
  labelKey: string
  color: string
  size: { w: number; h: number }
  inputs: boolean
  outputs: boolean
  defaultName: string
  defaultConfig: () => Record<string, unknown>
  fields: FieldSpec[]
  /** 画布节点上的一行摘要 */
  summary: (cfg: Record<string, any>) => string
}

const S = (cfg: Record<string, any>, k: string, d: string | number = '—'): string | number => cfg?.[k] ?? d

export const NODE_SPECS: Record<NodeType, NodeSpec> = {
  START: {
    type: 'START', group: 'trigger', labelKey: 'designer.tHttpStart', color: '#3D9E6B',
    size: { w: 132, h: 46 }, inputs: false, outputs: true,
    defaultName: '开始', defaultConfig: () => ({ description: '' }),
    fields: [{ key: 'description', labelKey: 'designer.fDesc', kind: 'text', placeholder: '触发说明' }],
    summary: (c) => String(S(c, 'description', '入口'))
  },
  END: {
    type: 'END', group: 'control', labelKey: 'designer.tEnd', color: '#B05A63',
    size: { w: 122, h: 44 }, inputs: true, outputs: false,
    defaultName: '结束', defaultConfig: () => ({}), fields: [],
    summary: () => '流程结束'
  },
  HTTP_INVOKE: {
    type: 'HTTP_INVOKE', group: 'invoke', labelKey: 'designer.tHttpInvoke', color: '#8A6FD4',
    size: { w: 176, h: 64 }, inputs: true, outputs: true,
    defaultName: 'HTTP 调用',
    defaultConfig: () => ({ connectionCode: '', operation: 'GET', timeoutMs: 5000, outputVar: 'resp' }),
    fields: [
      { key: 'connectionCode', labelKey: 'designer.fConnection', kind: 'text', placeholder: 'mock_orders' },
      { key: 'operation', labelKey: 'designer.fOperation', kind: 'text', placeholder: 'getOrder' },
      { key: 'outputVar', labelKey: 'designer.fOutputVar', kind: 'text', placeholder: 'resp', fallback: 'resp' },
      { key: 'timeoutMs', labelKey: 'designer.fTimeout', kind: 'number', min: 500, max: 60000, fallback: 5000 }
    ],
    summary: (c) => `${S(c, 'connectionCode', '未配置连接')} · ${S(c, 'timeoutMs', 5000)}ms`
  },
  TRANSFORM: {
    type: 'TRANSFORM', group: 'transform', labelKey: 'designer.tMapping', color: '#3B82C4',
    size: { w: 168, h: 62 }, inputs: true, outputs: true,
    defaultName: '数据映射', defaultConfig: () => ({ mapping: {}, outputVar: 'data' }),
    fields: [
      { key: 'outputVar', labelKey: 'designer.fOutputVar', kind: 'text', placeholder: 'data', fallback: 'data' },
      { key: 'mapping', labelKey: 'designer.fMapping', kind: 'textarea', placeholder: '{"target":"$.source"}' }
    ],
    summary: (c) => `${Object.keys(c?.mapping ?? {}).length} 个字段`
  },
  CHOICE: {
    type: 'CHOICE', group: 'route', labelKey: 'designer.tChoice', color: '#C99A3E',
    size: { w: 148, h: 62 }, inputs: true, outputs: false,
    defaultName: '条件分支', defaultConfig: () => ({}), fields: [],
    summary: (c: any) => `${c?.__branchCount ?? 0} 分支`
  },
  DELAY: {
    type: 'DELAY', group: 'control', labelKey: 'designer.tDelay', color: '#5C6B78',
    size: { w: 150, h: 56 }, inputs: true, outputs: true,
    defaultName: '延时', defaultConfig: () => ({ ms: 1000 }),
    fields: [{ key: 'ms', labelKey: 'designer.fDelayMs', kind: 'number', min: 0, max: 600000, fallback: 1000 }],
    summary: (c) => `${S(c, 'ms', 1000)} ms`
  },
  NOTIFY: {
    type: 'NOTIFY', group: 'control', labelKey: 'designer.tNotify', color: '#8A6FD4',
    size: { w: 168, h: 60 }, inputs: true, outputs: true,
    defaultName: '通知',
    defaultConfig: () => ({ channel: 'DINGTALK', template: '', to: '' }),
    fields: [
      { key: 'channel', labelKey: 'designer.fChannel', kind: 'select', fallback: 'DINGTALK',
        options: [
          { label: '钉钉', value: 'DINGTALK' },
          { label: '企业微信', value: 'WECOM' },
          { label: '邮件', value: 'EMAIL' },
          { label: 'Webhook', value: 'WEBHOOK' }
        ] },
      { key: 'template', labelKey: 'designer.fTemplate', kind: 'text', placeholder: 'big-order' },
      { key: 'to', labelKey: 'designer.fTo', kind: 'text', placeholder: '@finance' }
    ],
    summary: (c) => `${S(c, 'channel')} · ${S(c, 'template', '无模板')}`
  },
  SCRIPT: {
    type: 'SCRIPT', group: 'transform', labelKey: 'designer.tScript', color: '#3B82C4',
    size: { w: 168, h: 60 }, inputs: true, outputs: true,
    defaultName: '字段脚本', defaultConfig: () => ({ script: '', outputVar: 'out' }),
    fields: [
      { key: 'outputVar', labelKey: 'designer.fOutputVar', kind: 'text', fallback: 'out' },
      { key: 'script', labelKey: 'designer.fScript', kind: 'textarea', placeholder: 'return { a: input.b }' }
    ],
    summary: (c) => (c?.script ? '已配置脚本' : '未配置脚本')
  }
}

/** 调色板条目：分组 + 该组下所有已实现类型 + 规划中的占位项 */
export interface PaletteEntry {
  key: string
  labelKey: string
  color: string
  type?: NodeType
}

export const PALETTE: { group: GroupKey; groupKey: string; items: PaletteEntry[] }[] = [
  {
    group: 'trigger', groupKey: 'designer.groupTrigger',
    items: [
      { key: 'http', labelKey: 'designer.tHttpStart', color: '#3D9E6B', type: 'START' },
      { key: 'mq', labelKey: 'designer.tMqStart', color: '#3D9E6B' },
      { key: 'schedule', labelKey: 'designer.tSchedule', color: '#3D9E6B' }
    ]
  },
  {
    group: 'transform', groupKey: 'designer.groupTransform',
    items: [
      { key: 'map', labelKey: 'designer.tMapping', color: '#3B82C4', type: 'TRANSFORM' },
      { key: 'script', labelKey: 'designer.tScript', color: '#3B82C4', type: 'SCRIPT' }
    ]
  },
  {
    group: 'route', groupKey: 'designer.groupRoute',
    items: [
      { key: 'choice', labelKey: 'designer.tChoice', color: '#C99A3E', type: 'CHOICE' },
      { key: 'filter', labelKey: 'designer.tFilter', color: '#C99A3E' },
      { key: 'parallel', labelKey: 'designer.tParallel', color: '#C99A3E' }
    ]
  },
  {
    group: 'invoke', groupKey: 'designer.groupInvoke',
    items: [
      { key: 'http', labelKey: 'designer.tHttpInvoke', color: '#8A6FD4', type: 'HTTP_INVOKE' },
      { key: 'notify', labelKey: 'designer.tNotify', color: '#8A6FD4', type: 'NOTIFY' },
      { key: 'soap', labelKey: 'designer.tSoap', color: '#8A6FD4' },
      { key: 'db', labelKey: 'designer.tDb', color: '#8A6FD4' },
      { key: 'mqpub', labelKey: 'designer.tPublish', color: '#8A6FD4' }
    ]
  },
  {
    group: 'control', groupKey: 'designer.groupControl',
    items: [
      { key: 'delay', labelKey: 'designer.tDelay', color: '#5C6B78', type: 'DELAY' },
      { key: 'tx', labelKey: 'designer.tTransaction', color: '#5C6B78' },
      { key: 'sub', labelKey: 'designer.tSubflow', color: '#5C6B78' },
      { key: 'end', labelKey: 'designer.tEnd', color: '#B05A63', type: 'END' }
    ]
  }
]

export const specOf = (type: string): NodeSpec =>
  ACTIVE[type] ?? NODE_SPECS[type as string] ?? NODE_SPECS.HTTP_INVOKE

/** 节点摘要文案（CHOICE 需要分支数，由调用方补进 config） */
export function summaryOf(n: FlowNode): string {
  const cfg = { ...(n.config ?? {}), __branchCount: n.branches?.length ?? 0 }
  return specOf(n.type).summary(cfg)
}


/* ═══════════════════════════════════════════════════════════
 * 后端下发（S8）：manifest → NodeSpec 适配
 * 组件仍然只认 NodeSpec，因此画布/属性面板无需改动。
 * ═══════════════════════════════════════════════════════════ */

let ACTIVE: Record<string, NodeSpec> = NODE_SPECS

/** 当前生效的规格表（后端下发优先，内置兜底） */
export function setActiveSpecs(specs: Record<string, NodeSpec>) {
  ACTIVE = Object.keys(specs).length ? { ...NODE_SPECS, ...specs } : NODE_SPECS
}

export function resetActiveSpecs() {
  ACTIVE = NODE_SPECS
}

const GROUP_ORDER: GroupKey[] = ['trigger', 'transform', 'route', 'invoke', 'control']
const GROUP_KEY: Record<GroupKey, string> = {
  trigger: 'designer.groupTrigger',
  transform: 'designer.groupTransform',
  route: 'designer.groupRoute',
  invoke: 'designer.groupInvoke',
  control: 'designer.groupControl'
}

/** 取多语言字段，缺省回落 zh-CN */
function pick(map: Record<string, string> | undefined, locale: string): string {
  if (!map) return ''
  return map[locale] ?? map['zh-CN'] ?? Object.values(map)[0] ?? ''
}

/** 把 schema 的 properties 转成属性面板字段 */
function fieldsOf(schema?: {
  properties?: Record<string, any>
}): FieldSpec[] {
  if (!schema?.properties) return []
  return Object.entries(schema.properties).map(([key, prop]: [string, any]) => {
    const x = prop['x-eeip'] ?? {}
    const widget = x.widget ?? (prop.enum ? 'select' : prop.type === 'number' || prop.type === 'integer' ? 'number' : prop.type === 'object' || prop.type === 'boolean' ? 'textarea' : 'text')
    return {
      key,
      labelKey: x.labelKey ?? '',
      label: prop.title ?? key,
      kind: widget === 'select' ? 'select' : widget === 'number' ? 'number' : widget === 'code' || widget === 'json' ? 'textarea' : 'text',
      placeholder: x.placeholder,
      options: prop.enum?.map((v: any) => ({ label: String(v), value: String(v) })),
      min: prop.minimum,
      max: prop.maximum,
      fallback: prop.default
    } as FieldSpec & { label: string }
  })
}

/** 摘要模板：${config.x} / ${x} 替换；无模板时回落内置实现 */
function summaryTemplate(tpl: string | undefined) {
  if (!tpl) return null
  return (cfg: Record<string, any>) =>
    tpl.replace(/\$\{(?:config\.)?([a-zA-Z0-9_.]+)\}/g, (_, path) => {
      if (path === 'branches.length') return String(cfg.__branchCount ?? 0)
      if (path === 'mapping.size') return String(Object.keys(cfg.mapping ?? {}).length)
      const v = cfg[path]
      return v == null || v === '' ? '—' : String(v)
    })
}

/** manifest → NodeSpec */
export function adaptManifest(m: any, locale = 'zh-CN'): NodeSpec {
  const meta = m.meta ?? {}
  const size = meta.size ?? { w: 160, h: 60 }
  const props = m.config?.properties ?? {}
  const defaults: Record<string, unknown> = {}
  for (const [k, v] of Object.entries<any>(props)) {
    if (v?.default !== undefined) defaults[k] = v.default
  }
  const tplFn = summaryTemplate(meta.summary)
  return {
    type: m.type,
    group: (meta.category ?? 'invoke') as GroupKey,
    label: pick(meta.displayName, locale),
    labelKey: '',
    color: meta.color ?? '#8A6FD4',
    size: { w: size.w ?? 160, h: size.h ?? 60 },
    inputs: (m.ports?.inputs?.length ?? 0) > 0,
    outputs: (m.ports?.outputs ?? []).some((p: any) => p.type !== 'error'),
    defaultName: pick(meta.displayName, locale),
    defaultConfig: () => ({ ...defaults }),
    fields: fieldsOf(m.config) as FieldSpec[],
    summary: tplFn ?? ((cfg) => {
      const base = NODE_SPECS[m.type as NodeType]?.summary
      return base ? base(cfg) : ''
    })
  }
}

/** 批量适配 */
export function adaptManifests(list: any[], locale = 'zh-CN'): Record<string, NodeSpec> {
  const out: Record<string, NodeSpec> = {}
  for (const m of list) out[m.type] = adaptManifest(m, locale)
  return out
}

/** 调色板：按 manifest 分组生成；无 manifest 时用内置 PALETTE */
export function buildPalette(manifests: { type: string; meta?: any }[], locale = 'zh-CN') {
  if (!manifests.length) {
    return PALETTE.map((g) => ({
      group: g.group,
      groupKey: g.groupKey,
      items: g.items.map((i) => ({ key: i.key, label: '', labelKey: i.labelKey, color: i.color, type: i.type }))
    }))
  }
  const buckets = new Map<GroupKey, any[]>()
  for (const m of manifests) {
    const cat = (m.meta?.category ?? 'invoke') as GroupKey
    if (!buckets.has(cat)) buckets.set(cat, [])
    buckets.get(cat)!.push({
      key: m.type,
      label: pick(m.meta?.displayName, locale),
      labelKey: '',
      color: m.meta?.color ?? '#8A6FD4',
      type: m.type,
      order: m.meta?.order
    })
  }
  // 组内按 manifest 的 meta.order 排序（没有则排最后）
  for (const items of buckets.values()) {
    items.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  return GROUP_ORDER.filter((g) => buckets.has(g)).map((g) => ({
    group: g,
    groupKey: GROUP_KEY[g],
    items: buckets.get(g)!
  }))
}
