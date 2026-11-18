/**
 * 节点规格注册表 —— 编排器的唯一扩展点。
 *
 * 新增一种节点只需在这里加一条：调色板、画布配色与尺寸、属性面板字段、
 * 默认配置、连线路由全部由规格驱动，不需要改组件。
 * 后端同步：FlowValidator.NODE_TYPES + 类型专属校验。
 */
import type { FlowNode } from '@/api/flow'

export type NodeType =
  | 'START' | 'END' | 'HTTP_INVOKE' | 'TRANSFORM' | 'CHOICE'
  | 'DELAY' | 'NOTIFY' | 'SCRIPT'

/** 调色板分组 */
export type GroupKey = 'trigger' | 'transform' | 'route' | 'invoke' | 'control'

/** 属性面板字段类型 */
export type FieldKind = 'text' | 'number' | 'select' | 'textarea'

export interface FieldSpec {
  key: string
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
  /** 调色板与属性面板标题的 i18n key */
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
  NODE_SPECS[type as NodeType] ?? NODE_SPECS.HTTP_INVOKE

/** 节点摘要文案（CHOICE 需要分支数，由调用方补进 config） */
export function summaryOf(n: FlowNode): string {
  const cfg = { ...(n.config ?? {}), __branchCount: n.branches?.length ?? 0 }
  return specOf(n.type).summary(cfg)
}
