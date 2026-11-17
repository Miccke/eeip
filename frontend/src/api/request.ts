import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResult } from '@/types'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('eeip_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 后端按此语言返回错误文案与菜单名称
  config.headers['Accept-Language'] = localStorage.getItem('eeip_locale') || 'zh-CN'
  return config
})

service.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResult<unknown>
    if (body && body.code !== '0') {
      message.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    // 解包：调用方拿到的就是 data，不用再 .data
    return body.data as never
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401) {
      localStorage.removeItem('eeip_token')
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    } else if (status === 403) {
      message.error(body?.message || '无访问权限')
    } else if (!error.response) {
      message.error('网络异常，请检查后端服务是否启动')
    } else {
      message.error(body?.message || `请求失败（${status}）`)
    }
    return Promise.reject(error)
  }
)

export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as unknown as Promise<T>
}
export function post<T>(url: string, data?: object): Promise<T> {
  return service.post(url, data) as unknown as Promise<T>
}
export function put<T>(url: string, data?: object): Promise<T> {
  return service.put(url, data) as unknown as Promise<T>
}
export function del<T>(url: string): Promise<T> {
  return service.delete(url) as unknown as Promise<T>
}
export default service
