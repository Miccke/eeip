<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ title: string; desc: string }>()
const { t } = useI18n()
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ title }}</h1>
    <p class="eeip-page-desc">{{ desc }}</p>
    <div class="eeip-placeholder">
      <div style="font-size: 15px; margin-bottom: 6px">{{ t('common.functionOnlyDev') }}</div>
      <div style="font-size: 12px; opacity: .8"></div>
    </div>
  </div>
</template>
