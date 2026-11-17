<script setup lang="ts">
import { computed, watch } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import zhTW from 'ant-design-vue/es/locale/zh_TW'
import enUS from 'ant-design-vue/es/locale/en_US'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/zh-tw'
import 'dayjs/locale/en'
import { useAppStore } from '@/stores/app'
import { dayjsLocaleOf } from '@/i18n'

const appStore = useAppStore()

/** Ant Design Vue 的语言包 */
const ANT_LOCALES: Record<string, any> = { zh_CN: zhCN, zh_TW: zhTW, en_US: enUS }
const antLocale = computed(() => ({
  zh_CN: ANT_LOCALES.zh_CN,
  zh_TW: ANT_LOCALES.zh_TW,
  en_US: ANT_LOCALES.en_US
}[appStore.locale === 'zh-TW' ? 'zh_TW' : appStore.locale === 'en-US' ? 'en_US' : 'zh_CN']))

// 语言变化时同步 dayjs，保证日期选择器/分页文案一致
watch(() => appStore.locale, (l) => dayjs.locale(dayjsLocaleOf(l)), { immediate: true })

const themeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#f2a33c',
    colorInfo: '#f2a33c',
    colorSuccess: '#39cfbf',
    colorError: '#e5544b',
    colorBgBase: '#070a0e',
    colorBgContainer: '#0c1218',
    colorBorder: '#18222c',
    colorBorderSecondary: '#131c24',
    borderRadius: 2,
    fontSize: 13,
    controlHeight: 32
  }
}
</script>

<template>
  <a-config-provider :theme="themeConfig" :locale="antLocale">
    <router-view />
  </a-config-provider>
</template>
