import { get } from './request'
import type { PageResult } from '@/types'

/** 审计日志列表项 */
export interface AuditLogVO {
  id: string
  action: string
  actionName: string
  resourceType: string
  resourceId: string
  operatorId: string
  operatorName: string
  endpoint: string
  requestParams: string | null
  result: 'SUCCESS' | 'FAILED'
  errorMsg: string | null
  costMs: number | null
  clientIp: string | null
  occurredAt: string
}

/** 审计动作字典项 */
export interface AuditActionVO {
  code: string
  name: string
}

export interface AuditQuery {
  pageNum: number
  pageSize: number
  operator?: string
  action?: string
  result?: string
  startTime?: string
  endTime?: string
}

export const auditApi = {
  page: (params: AuditQuery) => get<PageResult<AuditLogVO>>('/v1/ops/audit-logs', params),
  actions: () => get<AuditActionVO[]>('/v1/ops/audit-logs/actions')
}
