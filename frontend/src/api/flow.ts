import { del, get, post, put } from './request'

/** 流程列表项 */
export interface FlowVO {
  id: string
  code: string
  name: string
  triggerType: string
  status: string
  version?: string
  nodeCount?: number
  dslHash?: string
  description?: string
  updatedAt?: string
}

export interface FlowNode {
  id: string
  type: string
  name?: string
  config?: Record<string, unknown>
  next?: string
  branches?: { when?: string; otherwise?: boolean; next: string }[]
}

export interface FlowDsl {
  schemaVersion: string
  flowCode?: string
  name?: string
  trigger?: Record<string, unknown>
  nodes: FlowNode[]
}

export interface FlowDetailVO {
  id: string
  code: string
  name: string
  triggerType: string
  status: string
  version?: string
  description?: string
  dsl?: FlowDsl
}

export interface FlowIssue {
  code: string
  message: string
  nodeId?: string
}

export interface FlowValidateResult {
  valid: boolean
  errors: FlowIssue[]
  warnings: FlowIssue[]
  nodeCount: number
  dslHash?: string
}

export interface FlowPayload {
  code: string
  name: string
  triggerType?: string
  description?: string
  changeLog?: string
  dsl: FlowDsl
}

export const flowApi = {
  list: (keyword?: string) => get<FlowVO[]>('/v1/integration/flows', { keyword }),
  detail: (id: string) => get<FlowDetailVO>(`/v1/integration/flows/${id}`),
  create: (data: FlowPayload) => post<string>('/v1/integration/flows', data),
  update: (id: string, data: FlowPayload) => put<void>(`/v1/integration/flows/${id}`, data),
  remove: (id: string) => del<void>(`/v1/integration/flows/${id}`),
  validate: (dsl: FlowDsl) => post<FlowValidateResult>('/v1/integration/flows/validate', dsl),
  publish: (id: string, changeLog?: string) =>
    post<FlowVO>(`/v1/integration/flows/${id}/publish${changeLog ? `?changeLog=${encodeURIComponent(changeLog)}` : ''}`),
  offline: (id: string) => post<void>(`/v1/integration/flows/${id}/offline`),
  versions: (id: string) => get<any[]>(`/v1/integration/flows/${id}/versions`)
}
