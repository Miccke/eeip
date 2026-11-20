import { get } from './request'

/** 流程实例 */
export interface InstanceVO {
  id: string
  flowId: string
  flowCode: string
  flowName?: string
  versionId?: string
  status: string
  triggerType: string
  costMs: number
  traceId?: string
  startedAt: string
  finishedAt?: string
  error?: string
}

/** 节点轨迹 */
export interface NodeTrace {
  seq: number
  nodeId: string
  nodeType: string
  nodeName?: string
  status: string
  inputJson?: string
  outputJson?: string
  error?: string
  costMs: number
  startedAt: string
}

export interface InstanceStats {
  total: number
  success: number
  failed: number
  running: number
  successRate: number
  avgCostMs: number
  maxCostMs: number
  hours: number
}

export interface InstanceQuery {
  flowId?: string
  status?: string
  triggerType?: string
  from?: string
  to?: string
  pageNum?: number
  pageSize?: number
}

export const instanceApi = {
  list: (q: InstanceQuery) => get<{ total: number; list: InstanceVO[] }>('/v1/integration/instances', q),
  stats: (hours = 24) => get<InstanceStats>('/v1/integration/instances/stats', { hours }),
  detail: (id: string) =>
    get<{ instance: InstanceVO; nodes: NodeTrace[] }>(`/v1/integration/instances/${id}`)
}
