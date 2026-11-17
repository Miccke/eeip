<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ApartmentOutlined, LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const store = useUserStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Admin@123', tenantCode: 'GROUP' })

async function onSubmit() {
  if (!form.username || !form.password) {
    message.warning(t('login.required'))
    return
  }
  loading.value = true
  try {
    await store.login(form.username, form.password, form.tenantCode || 'GROUP')
    await store.fetchMe()
    message.success(t('login.success'))
    router.push((route.query.redirect as string) || '/dashboard')
  } catch {
    /* 错误提示由拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="eeip-login">
    <section class="eeip-login__brand">
      <div class="eeip-login__grid" />
      <div style="position: relative; display: flex; align-items: center; gap: 12px">
        <span class="eeip-logo__mark"><i /></span>
        <span class="eeip-logo__text" style="letter-spacing: 0.22em">
          {{ t('app.name') }}<small>{{ t('app.subtitle') }}</small>
        </span>
      </div>

      <div>
        <div class="eeip-login__headline">
          {{ t('login.headline1') }}<br />{{ t('login.headline2') }}
        </div>
        <p class="eeip-login__sub">{{ t('login.intro') }}</p>
      </div>

      <div class="eeip-login__metrics">
        <div class="eeip-login__metric">
          <b>23</b><small>{{ t('login.metricSystems') }}</small><span>{{ t('login.metricSystemsSub') }}</span>
        </div>
        <div class="eeip-login__metric">
          <b>5,000</b><small>{{ t('login.metricTps') }}</small><span>{{ t('login.metricTpsSub') }}</span>
        </div>
        <div class="eeip-login__metric">
          <b>99.95</b><small>{{ t('login.metricSla') }}</small><span>{{ t('login.metricSlaSub') }}</span>
        </div>
        <div class="eeip-login__metric">
          <b>100</b><small>{{ t('login.metricAudit') }}</small><span>{{ t('login.metricAuditSub') }}</span>
        </div>
      </div>
    </section>

    <section class="eeip-login__form">
      <h2 style="font-size: 22px; margin: 0 0 6px">{{ t('login.title') }}</h2>
      <p style="opacity: 0.5; font-size: 12.5px; margin: 0 0 26px">{{ t('login.subtitle') }}</p>

      <a-form layout="vertical" :model="form" @finish="onSubmit">
        <a-form-item :label="t('login.username')" name="username">
          <a-input v-model:value="form.username" size="large" :placeholder="t('login.username')">
            <template #prefix><UserOutlined style="opacity: 0.45" /></template>
          </a-input>
        </a-form-item>
        <a-form-item :label="t('login.tenant')" name="tenantCode">
          <a-input v-model:value="form.tenantCode" size="large" placeholder="GROUP">
            <template #prefix><ApartmentOutlined style="opacity: 0.45" /></template>
          </a-input>
        </a-form-item>
        <a-form-item :label="t('login.password')" name="password">
          <a-input-password v-model:value="form.password" size="large" :placeholder="t('login.password')">
            <template #prefix><LockOutlined style="opacity: 0.45" /></template>
          </a-input-password>
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">
          {{ t('login.submit') }}
        </a-button>
      </a-form>

      <div class="eeip-login__tip">
        {{ t('login.tipTitle') }}<br />
        GROUP · admin / Admin@123 —— {{ t('login.tipAdmin') }}<br />
        GROUP · mgr01 / Admin@123 —— {{ t('login.tipDept') }}<br />
        SUPPLIER_A · admin / Admin@123 —— {{ t('login.tipPartner') }}
      </div>
    </section>
  </div>
</template>
