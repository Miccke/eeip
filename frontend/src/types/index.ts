/** 后端雪花 ID：序列化为字符串，避免 JS 精度丢失 */
export type ID = string

export interface ApiResult<T> {
  code: string
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  list: T[]
}

export interface MenuVO {
  id: ID
  name: string
  path: string | null
  component: string | null
  icon: string | null
  type: 'DIR' | 'MENU'
  hidden: boolean
  children?: MenuVO[]
}

export interface LoginResp {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface UserInfo {
  id: ID
  username: string
  nickname: string
  avatar: string | null
  deptName: string | null
  roles: string[]
  perms: string[]
  menus: MenuVO[]
}

export interface UserVO {
  id: ID
  username: string
  nickname: string
  email: string | null
  phone: string | null
  deptName: string | null
  status: number
  roleIds: ID[]
  roleNames: string[]
  lastLoginAt: string | null
  createdAt: string | null
  remark: string | null
}

export interface RoleVO {
  id: ID
  code: string
  name: string
  dataScope: string
  sort: number
  status: number
  builtin: boolean
  remark: string | null
  userCount: number
}

export interface PermNodeVO {
  id: ID
  parentId: ID
  type: 'DIR' | 'MENU' | 'BUTTON'
  code: string | null
  name: string
  path: string | null
  icon: string | null
  sort: number
  children: PermNodeVO[]
}
