import { del, get, post, put } from './request'

/** 租户列表项 */
export interface TenantVO {
  id: string
  code: string
  name: string
  tenantType: 'INTERNAL' | 'PARTNER'
  status: number
  contactName: string | null
  contactPhone: string | null
  expireAt: string | null
  remark: string | null
  createdAt: string | null
}

export interface TenantPayload {
  code: string
  name: string
  tenantType?: string
  status?: number
  contactName?: string
  contactPhone?: string
  expireAt?: string
  remark?: string
}

export const tenantApi = {
  list: (keyword?: string) => get<TenantVO[]>('/v1/system/tenants', { keyword }),
  create: (data: TenantPayload) => post<number>('/v1/system/tenants', data),
  update: (id: string, data: TenantPayload) => put<void>(`/v1/system/tenants/${id}`, data),
  remove: (id: string) => del<void>(`/v1/system/tenants/${id}`)
}
