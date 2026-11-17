import { del, get, post, put } from './request'
import type { PageResult, UserVO } from '@/types'

export interface UserQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number
}

export interface UserPayload {
  username: string
  nickname: string
  password?: string
  email?: string
  phone?: string
  deptName?: string
  status?: number
  roleIds?: string[]
  remark?: string
}

export const userApi = {
  page: (params: UserQuery) => get<PageResult<UserVO>>('/v1/system/users', params),
  create: (data: UserPayload) => post<number>('/v1/system/users', data),
  update: (id: string, data: UserPayload) => put<void>(`/v1/system/users/${id}`, data),
  remove: (id: string) => del<void>(`/v1/system/users/${id}`),
  resetPassword: (id: string, password: string) =>
    put<void>(`/v1/system/users/${id}/password`, { password })
}
