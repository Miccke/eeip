import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/index.css'

import App from './App.vue'
import router from './router'
import { setupGuard } from './router/guard'
import { setupDirectives } from './directives/permission'
import i18n from './i18n'

const app = createApp(App)
app.use(createPinia())
app.use(i18n)
app.use(router)
app.use(Antd)
setupDirectives(app)
setupGuard(router)

app.mount('#app')
