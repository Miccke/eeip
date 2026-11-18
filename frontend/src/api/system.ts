import { del, get, post, put } from './request'

export interface SystemVO {
  id: string
  code: string
  name: string
  env: string
  protocol: string
  baseUrl?: string
  owner?: string
  ownerPhone?: string
  status: number
  connectorCount: number
  apiCount: number
  remark?: string
  updatedAt?: string
}

export interface SystemPayload {
  code: string
  name: string
  env?: string
  protocol?: string
  baseUrl?: string
  owner?: string
  ownerPhone?: string
  status?: number
  remark?: string
}

export const systemApi = {
  list: (keyword?: string) => get<SystemVO[]>('/v1/integration/systems', { keyword }),
  create: (data: SystemPayload) => post<string>('/v1/integration/systems', data),
  update: (id: string, data: SystemPayload) => put<void>(`/v1/integration/systems/${id}`, data),
  remove: (id: string) => del<void>(`/v1/integration/systems/${id}`)
}
