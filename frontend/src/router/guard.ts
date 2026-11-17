import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function setupGuard(router: Router) {
  router.beforeEach(async (to) => {
    const store = useUserStore()
    if (to.meta.public) {
      return true
    }
    if (!store.token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (!store.loaded) {
      try {
        await store.fetchMe()
      } catch {
        store.reset()
        return { path: '/login' }
      }
    }
    // 菜单中没有的路径一律回首页，避免直接输 URL 越权访问页面
    if (!store.canAccess(to.path)) {
      return { path: '/dashboard', query: { denied: to.path } }
    }
    return true
  })
}
