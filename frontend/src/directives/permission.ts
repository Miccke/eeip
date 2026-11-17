import type { App, Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/** v-hasPerm="'system:user:add'" 或 v-hasPerm="['a','b']"（任一命中即显示） */
const hasPerm: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const store = useUserStore()
    if (!store.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function setupDirectives(app: App) {
  app.directive('hasPerm', hasPerm)
}
