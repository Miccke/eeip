<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Placeholder from '@/components/Placeholder.vue'

const { t } = useI18n()
</script>

<template>
  <Placeholder :title="t('placeholder.apiTitle')" :desc="t('placeholder.apiDesc')" />
</template>
