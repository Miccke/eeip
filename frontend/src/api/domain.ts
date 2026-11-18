import { del, get, post, put } from './request'

export interface Domain {
  id: string
  code: string
  name: string
  nameEn?: string
  sort: number
  status: number
  remark?: string
}

export const domainApi = {
  list: () => get<Domain[]>('/v1/integration/domains'),
  create: (d: Partial<Domain>) => post<string>('/v1/integration/domains', d),
  update: (id: string, d: Partial<Domain>) => put<void>(`/v1/integration/domains/${id}`, d),
  remove: (id: string) => del<void>(`/v1/integration/domains/${id}`)
}
