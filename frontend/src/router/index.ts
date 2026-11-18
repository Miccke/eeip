import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

export const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/login/index.vue'), meta: { public: true } },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { titleKey: 'dashboard.title' } },
      { path: 'asset/api', name: 'asset-api', component: () => import('@/views/asset/api/index.vue'), meta: { titleKey: 'placeholder.apiTitle' } },
      { path: 'asset/flow', name: 'asset-flow', component: () => import('@/views/asset/flow/index.vue'), meta: { titleKey: 'placeholder.flowTitle' } },
      { path: 'asset/flow/:id', name: 'asset-flow-designer', component: () => import('@/views/asset/flow/Designer.vue'), meta: { titleKey: 'placeholder.flowTitle' } },
      { path: 'asset/connection', name: 'asset-connection', component: () => import('@/views/asset/connection/index.vue'), meta: { titleKey: 'conn.title' } },
      { path: 'ops/console', name: 'ops-console', component: () => import('@/views/ops/console/index.vue'), meta: { titleKey: 'placeholder.consoleTitle' } },
      { path: 'ops/dlq', name: 'ops-dlq', component: () => import('@/views/ops/dlq/index.vue'), meta: { titleKey: 'placeholder.dlqTitle' } },
      { path: 'ops/audit', name: 'ops-audit', component: () => import('@/views/ops/audit/index.vue'), meta: { titleKey: 'audit.title' } },
      { path: 'system/user', name: 'system-user', component: () => import('@/views/system/user/index.vue'), meta: { titleKey: 'user.title' } },
      { path: 'system/role', name: 'system-role', component: () => import('@/views/system/role/index.vue'), meta: { titleKey: 'role.title' } },
      { path: 'system/tenant', name: 'system-tenant', component: () => import('@/views/system/tenant/index.vue'), meta: { titleKey: 'tenant.title' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/error/404.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
