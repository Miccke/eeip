<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'
import { DownOutlined, GlobalOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { iconOf } from '@/utils/icon'
import { LOCALES, type LocaleCode } from '@/i18n'

const { t } = useI18n()
const store = useUserStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const { info, menus } = storeToRefs(store)
const collapsed = ref(false)
const openKeys = ref<string[]>([])

const breadcrumb = computed(() => {
  const key = route.meta.titleKey as string | undefined
  return key ? [t('app.name'), t(key)] : [t('app.name')]
})

watch(
  () => route.path,
  (path) => {
    const seg = path.split('/').filter(Boolean)
    if (seg.length > 1) openKeys.value = ['/' + seg[0]]
  },
  { immediate: true }
)

async function onLogout() {
  await store.logout()
  router.push('/login')
}

/** 切换语言：更新 i18n 后重新拉取 /auth/me，让后端返回的菜单名也跟着变 */
async function onLocale(code: LocaleCode) {
  appStore.setLocale(code)
  if (store.token && store.loaded) {
    await store.fetchMe()
  }
}
</script>

<template>
  <a-layout style="height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :width="224"
      :collapsed-width="64"
      collapsible
      theme="dark"
      style="border-right: 1px solid var(--eeip-line)"
    >
      <div class="eeip-logo">
        <span class="eeip-logo__mark"><i /></span>
        <span v-show="!collapsed" class="eeip-logo__text">
          {{ t('app.name') }}<small>{{ t('app.subtitle') }}</small>
        </span>
      </div>

      <a-menu v-model:open-keys="openKeys" theme="dark" mode="inline" :selected-keys="[route.path]">
        <template v-for="m in menus" :key="m.id">
          <a-sub-menu v-if="m.type === 'DIR' && m.children?.length" :key="m.path || String(m.id)">
            <template #icon><component :is="iconOf(m.icon)" /></template>
            <template #title>{{ m.name }}</template>
            <a-menu-item v-for="c in m.children" :key="c.path || String(c.id)">
              <router-link :to="c.path || '/'">{{ c.name }}</router-link>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="m.path || String(m.id)">
            <template #icon><component :is="iconOf(m.icon)" /></template>
            <router-link :to="m.path || '/'">{{ m.name }}</router-link>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <header class="eeip-header">
        <a-breadcrumb>
          <a-breadcrumb-item v-for="(b, i) in breadcrumb" :key="i">{{ b }}</a-breadcrumb-item>
        </a-breadcrumb>
        <span class="eeip-header__spacer" />

        <a-dropdown>
          <a style="display: flex; align-items: center; gap: 6px; font-size: 12px" @click.prevent>
            <GlobalOutlined />
            {{ LOCALES.find((l) => l.value === appStore.locale)?.label }}
          </a>
          <template #overlay>
            <a-menu :selected-keys="[appStore.locale]">
              <a-menu-item
                v-for="l in LOCALES"
                :key="l.value"
                @click="onLocale(l.value as LocaleCode)"
              >
                {{ l.label }}
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>

        <span class="eeip-env">{{ t('app.env') }}</span>

        <a-dropdown>
          <a style="display: flex; align-items: center; gap: 8px" @click.prevent>
            <a-avatar size="small" style="background: #1b2530">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <span style="font-size: 12.5px">{{ info?.nickname || info?.username }}</span>
            <DownOutlined style="font-size: 10px; opacity: 0.5" />
          </a>
          <template #overlay>
            <a-menu>
              <a-menu-item disabled>{{ info?.roles?.join(' / ') }}</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" @click="onLogout">
                <LogoutOutlined /> {{ t('app.logout') }}
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </header>

      <a-layout-content class="eeip-content">
        <router-view v-slot="{ Component }">
          <!-- 用 fullPath 做 key：新增/查看详情切换时会重建组件，画布不会残留上一条流程 -->
          <keep-alive :max="8">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
