import { createI18n } from 'vue-i18n'
import zhCN from '@/locales/zh-CN'
import zhTW from '@/locales/zh-TW'
import enUS from '@/locales/en-US'

/** 支持的三种语言 */
export const LOCALES = [
  { value: 'zh-CN', label: '简体中文', antd: 'zh_CN', dayjs: 'zh-cn' },
  { value: 'zh-TW', label: '繁體中文', antd: 'zh_TW', dayjs: 'zh-tw' },
  { value: 'en-US', label: 'English', antd: 'en_US', dayjs: 'en' }
] as const

export type LocaleCode = (typeof LOCALES)[number]['value']

const STORAGE_KEY = 'eeip_locale'

/** 读取本地保存的语言，默认简体中文 */
export function getSavedLocale(): LocaleCode {
  const saved = localStorage.getItem(STORAGE_KEY) as LocaleCode | null
  return LOCALES.some((l) => l.value === saved) ? (saved as LocaleCode) : 'zh-CN'
}

export function saveLocale(locale: LocaleCode) {
  localStorage.setItem(STORAGE_KEY, locale)
}

/** 取某种语言对应的 Ant Design Vue locale 包名 */
export function antdLocaleOf(code: string) {
  return LOCALES.find((l) => l.value === code)?.antd ?? 'zh_CN'
}

/** 取某种语言对应的 dayjs locale */
export function dayjsLocaleOf(code: string) {
  return LOCALES.find((l) => l.value === code)?.dayjs ?? 'zh-cn'
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: getSavedLocale(),
  fallbackLocale: 'zh-CN',
  missingWarn: false,
  fallbackWarn: false,
  messages: {
    'zh-CN': zhCN,
    'zh-TW': zhTW,
    'en-US': enUS
  }
})

export default i18n
