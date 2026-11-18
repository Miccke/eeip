<script setup lang="ts">
/** 流程列表：进入设计器 / 发布 / 下线 / 删除 */
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { flowApi, type FlowVO } from '@/api/flow'

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const rows = ref<FlowVO[]>([])
const keyword = ref('')

const columns = computed(() => [
  { title: t('flow.colCode'), dataIndex: 'code', key: 'code', width: 180 },
  { title: t('flow.colName'), dataIndex: 'name', key: 'name', width: 220 },
  { title: t('flow.colStatus'), key: 'status', width: 120 },
  { title: t('flow.colVersion'), key: 'version', width: 100 },
  { title: t('flow.colNodes'), dataIndex: 'nodeCount', key: 'nodeCount', width: 90 },
  { title: t('flow.colHash'), dataIndex: 'dslHash', key: 'dslHash', width: 130 },
  { title: t('flow.updatedAt') ?? t('common.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: t('common.actions'), key: 'action', width: 220, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await flowApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

const openDesigner = (row?: FlowVO) =>
  router.push(row ? `/asset/flow/${row.id}` : { path: '/asset/flow/new', query: { mode: 'new' } })

async function publish(row: FlowVO) {
  try {
    const r = await flowApi.publish(row.id)
    message.success(t('flow.published', { v: r.version }))
    load()
  } catch { /* 拦截器已提示 */ }
}

async function offline(row: FlowVO) {
  await flowApi.offline(row.id)
  message.success(t('flow.offlined'))
  load()
}

function remove(row: FlowVO) {
  Modal.confirm({
    title: t('flow.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() {
      await flowApi.remove(row.id)
      message.success(t('flow.deleted'))
      load()
    }
  })
}

const statusColor = (s: string) =>
  s === 'PUBLISHED' ? 'success' : s === 'OFFLINE' ? 'default' : 'warning'

const fmt = (x?: string) => (x ? x.replace('T', ' ').slice(0, 19) : '—')

onMounted(load)
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('flow.title') }}</h1>
    <p class="eeip-page-desc">{{ t('flow.desc') }}</p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('flow.searchPlaceholder')" style="width: 220px"
               allow-clear @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'asset:flow:create'" type="primary" @click="openDesigner()">
        <PlusOutlined /> {{ t('flow.create') }}
      </a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 1200 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <span style="font-family: monospace; font-size: 12px">{{ record.code }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="statusColor(record.status)" :text="t('flow.status' + record.status)" />
        </template>
        <template v-else-if="column.key === 'version'">
          <a-tag v-if="record.version" color="orange">{{ record.version }}</a-tag>
          <span v-else style="opacity: .4">—</span>
        </template>
        <template v-else-if="column.key === 'dslHash'">
          <span style="font-family: monospace; font-size: 11px; opacity: .6">{{ record.dslHash || '—' }}</span>
        </template>
        <template v-else-if="column.key === 'updatedAt'">
          <span style="font-size: 11.5px; opacity: .7">{{ fmt(record.updatedAt) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'asset:flow:update'" @click="openDesigner(record)">{{ t('flow.openDesigner') }}</a>
            <a v-hasPerm="'asset:flow:publish'" @click="publish(record)">{{ t('flow.publish') }}</a>
            <a v-if="record.status === 'PUBLISHED'" v-hasPerm="'asset:flow:publish'"
               @click="offline(record)">{{ t('flow.offline') }}</a>
            <a v-hasPerm="'asset:flow:delete'" style="color: #e5544b" @click="remove(record)">
              {{ t('common.delete') }}
            </a>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
