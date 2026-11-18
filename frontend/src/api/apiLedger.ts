import { del, get, post, put } from './request'

export interface ApiVO {
  id: string
  apiNo?: string
  systemCode: string
  protocol?: string
  slaLevel?: string
  domainCode?: string
  targetType?: string
  flowCode?: string
  code: string
  name: string
  status: string
  version?: string
  method?: string
  path?: string
  connectorCodes: string[]
  publicPath?: string
  description?: string
  updatedAt?: string
}

export interface ApiPayload {
  systemCode: string
  protocol?: string
  slaLevel?: string
  targetType?: string
  flowCode?: string
  code: string
  name: string
  method?: string
  path: string
  connectorCodes?: string[]
  changeLog?: string
  description?: string
}

export const apiLedgerApi = {
  list: (systemCode?: string, keyword?: string) =>
    get<ApiVO[]>('/v1/integration/apis', { systemCode, keyword }),
  create: (data: ApiPayload) => post<string>('/v1/integration/apis', data),
  update: (id: string, data: ApiPayload) => put<void>(`/v1/integration/apis/${id}`, data),
  publish: (id: string) => post<ApiVO>(`/v1/integration/apis/${id}/publish`),
  offline: (id: string) => post<void>(`/v1/integration/apis/${id}/offline`),
  remove: (id: string) => del<void>(`/v1/integration/apis/${id}`)
}
