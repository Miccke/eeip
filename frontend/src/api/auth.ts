import { get, post } from './request'
import type { LoginResp, UserInfo } from '@/types'

export const authApi = {
  login: (data: { username: string; password: string; tenantCode?: string }) =>
    post<LoginResp>('/v1/auth/login', data),
  me: () => get<UserInfo>('/v1/auth/me'),
  logout: () => post<void>('/v1/auth/logout')
}
