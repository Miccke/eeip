<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const store = useUserStore()

const permCount = computed(() => store.info?.perms?.length ?? 0)
const menuCount = computed(() => {
  const count = (list: any[]): number =>
    list.reduce((n, m) => n + 1 + (m.children ? count(m.children) : 0), 0)
  return count(store.info?.menus ?? [])
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('dashboard.title') }}</h1>
    <p class="eeip-page-desc">
      {{ t('dashboard.welcome', { name: store.info?.nickname, roles: store.info?.roles?.join(' / ') }) }}
    </p>

    <div class="eeip-stat-grid">
      <div class="eeip-stat">
        <div class="eeip-stat__label">{{ t('dashboard.menuCount') }}</div>
        <div class="eeip-stat__value">{{ menuCount }}<small>{{ t('dashboard.unitItem') }}</small></div>
      </div>
      <div class="eeip-stat">
        <div class="eeip-stat__label">{{ t('dashboard.permCount') }}</div>
        <div class="eeip-stat__value">{{ permCount }}<small>{{ t('dashboard.unitPerm') }}</small></div>
      </div>
      <div class="eeip-stat">
        <div class="eeip-stat__label">{{ t('dashboard.availability') }}</div>
        <div class="eeip-stat__value">99.95<small>%</small></div>
      </div>
      <div class="eeip-stat">
        <div class="eeip-stat__label">{{ t('dashboard.throughput') }}</div>
        <div class="eeip-stat__value">1,284<small>TPS</small></div>
      </div>
    </div>

    <a-alert
      style="margin-top: 18px"
      type="info"
      show-icon
      :message="t('dashboard.noticeTitle')"
      :description="t('dashboard.noticeBody')"
    />
  </div>
</template>
