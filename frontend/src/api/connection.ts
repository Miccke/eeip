import { del, get, post, put } from './request'

/** 连接列表项（不含凭证） */
export interface ConnectionVO {
  id: string
  code: string
  name: string
  type: string
  endpoint: string
  method: string
  authType: string
  hasAuth: boolean
  status: number
  lastCheckStatus?: string
  lastCheckMs?: number
  lastCheckError?: string
  lastCheckAt?: string
  remark?: string
}

/** 连通性测试结果 */
export interface ConnectionTestResult {
  success: boolean
  status?: number
  costMs: number
  bodySnippet?: string
  headers?: Record<string, string>
  error?: string
}

export interface ConnectionPayload {
  code: string
  name: string
  endpoint: string
  method?: string
  authType?: string
  authConfig?: string
  headersJson?: string
  timeoutMs?: number
  status?: number
  remark?: string
}

export const connectionApi = {
  list: (keyword?: string) => get<ConnectionVO[]>('/v1/integration/connections', { keyword }),
  create: (data: ConnectionPayload) => post<number>('/v1/integration/connections', data),
  update: (id: string, data: ConnectionPayload) =>
    put<void>(`/v1/integration/connections/${id}`, data),
  remove: (id: string) => del<void>(`/v1/integration/connections/${id}`),
  test: (id: string) => post<ConnectionTestResult>(`/v1/integration/connections/${id}/test`),
  testDraft: (data: ConnectionPayload) =>
    post<ConnectionTestResult>('/v1/integration/connections/test-draft', data)
}
