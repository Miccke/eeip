import { defineStore } from 'pinia'
import { ref } from 'vue'
import i18n, { getSavedLocale, saveLocale, type LocaleCode } from '@/i18n'

/**
 * 全局应用状态：当前语言等。
 */
export const useAppStore = defineStore('app', () => {
  const locale = ref<LocaleCode>(getSavedLocale())

  /** 切换语言：同步 vue-i18n、localStorage；调用方负责切换 antd/dayjs 与刷新菜单 */
  function setLocale(next: LocaleCode) {
    locale.value = next
    i18n.global.locale.value = next
    saveLocale(next)
  }

  return { locale, setLocale }
})
