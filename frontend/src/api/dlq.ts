import { get, post } from './request'

/** 死信 */
export interface DeadLetter {
  id: string
  instanceId: string
  flowId: string
  flowCode?: string
  flowName?: string
  versionId?: string
  nodeId?: string
  nodeName?: string
  nodeType?: string
  error?: string
  inputJson?: string
  attempts: number
  status: string
  retriedInstance?: string
  retryCount: number
  handledBy?: string
  handledAt?: string
  remark?: string
  createdAt: string
}

export interface DlqStats {
  total: number
  pending: number
  retried: number
  discarded: number
}

export const dlqApi = {
  list: (q: { status?: string; flowCode?: string; pageNum?: number; pageSize?: number }) =>
    get<{ total: number; list: DeadLetter[] }>('/v1/integration/dead-letters', q),
  stats: () => get<DlqStats>('/v1/integration/dead-letters/stats'),
  detail: (id: string) => get<DeadLetter>(`/v1/integration/dead-letters/${id}`),
  retry: (id: string) => post<any>(`/v1/integration/dead-letters/${id}/retry`),
  discard: (id: string, remark?: string) =>
    post<void>(`/v1/integration/dead-letters/${id}/discard${remark ? `?remark=${encodeURIComponent(remark)}` : ''}`)
}
