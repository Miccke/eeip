import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { MenuVO, UserInfo } from '@/types'

const TOKEN_KEY = 'eeip_token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const info = ref<UserInfo | null>(null)
  const loaded = ref(false)

  const perms = computed(() => new Set(info.value?.perms ?? []))
  const roles = computed(() => info.value?.roles ?? [])
  const menus = computed<MenuVO[]>(() => info.value?.menus ?? [])

  /** 当前用户可访问的路由路径集合（由后端菜单反推） */
  const allowedPaths = computed<Set<string>>(() => {
    const set = new Set<string>()
    const walk = (list: MenuVO[]) => {
      list.forEach((m) => {
        if (m.path) set.add(m.path)
        if (m.children?.length) walk(m.children)
      })
    }
    walk(info.value?.menus ?? [])
    return set
  })

  function canAccess(path: string): boolean {
    if (path === '/' || path === '/dashboard') return true
    if (allowedPaths.value.has(path)) return true
    // 目录路径（如 /system）在菜单中存在即允许
    return Array.from(allowedPaths.value).some((p) => p.startsWith(path + '/'))
  }

  function hasPerm(perm?: string | string[]): boolean {
    if (!perm) return true
    const need = Array.isArray(perm) ? perm : [perm]
    return need.some((p) => perms.value.has(p) || perms.value.has('*'))
  }

  async function login(username: string, password: string, tenantCode?: string) {
    const res = await authApi.login({ username, password, tenantCode })
    token.value = res.accessToken
    localStorage.setItem(TOKEN_KEY, res.accessToken)
  }

  async function fetchMe() {
    info.value = await authApi.me()
    loaded.value = true
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      /* 忽略登出异常 */
    }
    reset()
  }

  function reset() {
    token.value = ''
    info.value = null
    loaded.value = false
    localStorage.removeItem(TOKEN_KEY)
  }

  return {
    token, info, loaded, perms, roles, menus, allowedPaths,
    hasPerm, canAccess, login, fetchMe, logout, reset
  }
})
