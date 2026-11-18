import { del, get, post, put } from './request'

/** 对外暴露的接口 */
export interface OpenApiVO {
  id: string
  code: string
  name: string
  method: string
  path: string
  fullPath: string
  targetType: 'PROXY' | 'FLOW'
  connectionCode?: string
  upstreamPath?: string
  authType: 'NONE' | 'API_KEY'
  hasKey: boolean
  qps: number
  status: number
  version: string
  remark?: string
  updatedAt?: string
}

export interface OpenApiPayload {
  code: string
  name: string
  method: string
  path: string
  targetType?: string
  connectionCode?: string
  upstreamPath?: string
  upstreamMethod?: string
  headersJson?: string
  authType?: string
  apiKey?: string
  qps?: number
  status?: number
  version?: string
  remark?: string
}

export const openApiApi = {
  list: (keyword?: string) => get<OpenApiVO[]>('/v1/integration/open-apis', { keyword }),
  create: (data: OpenApiPayload) => post<string>('/v1/integration/open-apis', data),
  update: (id: string, data: OpenApiPayload) => put<void>(`/v1/integration/open-apis/${id}`, data),
  remove: (id: string) => del<void>(`/v1/integration/open-apis/${id}`),
  resetKey: (id: string) => post<string>(`/v1/integration/open-apis/${id}/reset-key`)
}
