import { del, get, post, put } from './request'
import type { PermNodeVO, RoleVO } from '@/types'

export interface RolePayload {
  code: string
  name: string
  dataScope?: string
  sort?: number
  status?: number
  remark?: string
}

export const roleApi = {
  list: (keyword?: string) => get<RoleVO[]>('/v1/system/roles', { keyword }),
  create: (data: RolePayload) => post<number>('/v1/system/roles', data),
  update: (id: string, data: RolePayload) => put<void>(`/v1/system/roles/${id}`, data),
  remove: (id: string) => del<void>(`/v1/system/roles/${id}`),
  permissionIds: (id: string) => get<string[]>(`/v1/system/roles/${id}/permissions`),
  assign: (id: string, permissionIds: string[]) =>
    put<void>(`/v1/system/roles/${id}/permissions`, { permissionIds }),
  permissionTree: () => get<PermNodeVO[]>('/v1/system/permissions/tree')
}
