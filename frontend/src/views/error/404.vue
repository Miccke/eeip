<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <div style="height: 100vh; display: grid; place-items: center; background: #070a0e">
    <div style="text-align: center">
      <div style="font-size: 56px; font-family: monospace; opacity: 0.35">404</div>
      <p style="opacity: 0.6; margin: 8px 0 20px">{{ t('err.notFound') }}</p>
      <a-button type="primary" href="/">{{ t('err.backHome') }}</a-button>
    </div>
  </div>
</template>
