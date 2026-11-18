import { del, get, post, put } from './request'

export interface AppClientVO {
  id: string
  appKey: string
  appName: string
  owner?: string
  status: number
  expireAt?: string
  allowedIps?: string
  grantedCount: number
  remark?: string
  createdAt?: string
}

export interface AppClientPayload {
  appKey?: string
  appName: string
  owner?: string
  status?: number
  allowedIps?: string
  remark?: string
}

export const appClientApi = {
  list: (keyword?: string) => get<AppClientVO[]>('/v1/system/app-clients', { keyword }),
  create: (data: AppClientPayload) =>
    post<{ id: string; appKey: string; appSecret: string }>('/v1/system/app-clients', data),
  update: (id: string, data: AppClientPayload) => put<void>(`/v1/system/app-clients/${id}`, data),
  remove: (id: string) => del<void>(`/v1/system/app-clients/${id}`),
  resetSecret: (id: string) => post<string>(`/v1/system/app-clients/${id}/reset-secret`),
  grants: (id: string) => get<string[]>(`/v1/system/app-clients/${id}/grants`),
  saveGrants: (id: string, apiIds: string[]) => put<void>(`/v1/system/app-clients/${id}/grants`, apiIds)
}
