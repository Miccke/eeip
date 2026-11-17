<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { auditApi, type AuditActionVO, type AuditLogVO } from '@/api/audit'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<AuditLogVO[]>([])
const total = ref(0)
const actions = ref<AuditActionVO[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 20,
  operator: '',
  action: undefined as string | undefined,
  result: undefined as string | undefined
})
const range = ref<[any, any] | null>(null)

const columns = computed(() => [
  { title: t('audit.colTime'), dataIndex: 'occurredAt', key: 'occurredAt', width: 175 },
  { title: t('audit.colOperator'), key: 'operator', width: 150 },
  { title: t('audit.colAction'), key: 'action', width: 190 },
  { title: t('audit.colResource'), key: 'resource', width: 200 },
  { title: t('audit.colResult'), key: 'result', width: 100 },
  { title: t('audit.colCost'), dataIndex: 'costMs', key: 'costMs', width: 90 },
  { title: t('audit.colIp'), dataIndex: 'clientIp', key: 'clientIp', width: 130 }
])

const actionOptions = computed(() =>
  actions.value.map((a) => ({ label: `${a.name || a.code}（${a.code}）`, value: a.code }))
)

async function load() {
  loading.value = true
  try {
    const [start, end] = range.value ?? []
    const res = await auditApi.page({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      operator: query.operator || undefined,
      action: query.action,
      result: query.result,
      startTime: start ? start.toISOString() : undefined,
      endTime: end ? end.toISOString() : undefined
    })
    rows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}

function onTableChange(p: any) {
  query.pageNum = p.current
  query.pageSize = p.pageSize
  load()
}

onMounted(async () => {
  await load()
  actions.value = await auditApi.actions()
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('audit.title') }}</h1>
    <p class="eeip-page-desc">
      {{ t('audit.desc') }}
    </p>

    <a-space style="margin-bottom: 12px" wrap>
      <a-input
        v-model:value="query.operator"
        :placeholder="t('audit.operatorPlaceholder')"
        style="width: 190px"
        allow-clear
        @press-enter="((query.pageNum = 1), load())"
      >
        <template #prefix><SearchOutlined style="opacity: 0.4" /></template>
      </a-input>

      <a-select
        v-model:value="query.action"
        :options="actionOptions"
        :placeholder="t('audit.actionAll')"
        style="width: 230px"
        allow-clear
      />

      <a-select
        v-model:value="query.result"
        :placeholder="t('audit.resultAll')"
        style="width: 140px"
        allow-clear
        :options="[
          { label: t('common.success'), value: 'SUCCESS' },
          { label: t('common.failed'), value: 'FAILED' }
        ]"
      />

      <a-range-picker v-model:value="range" show-time style="width: 340px" />

      <a-button type="primary" @click="((query.pageNum = 1), load())">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
    </a-space>

    <a-table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      size="middle"
      :scroll="{ x: 1100 }"
      :pagination="{
        current: query.pageNum,
        pageSize: query.pageSize,
        total,
        showSizeChanger: true,
        showTotal: (total: number) => t('common.total', { n: total })
      }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'occurredAt'">
          <span style="font-family: monospace; font-size: 11.5px">{{ fmt(record.occurredAt) }}</span>
        </template>
        <template v-else-if="column.key === 'operator'">
          {{ record.operatorName }}
          <span style="opacity: 0.4; font-size: 11px">#{{ record.operatorId }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <div>{{ record.actionName || record.action }}</div>
          <div style="font-family: monospace; font-size: 10.5px; opacity: 0.4">{{ record.action }}</div>
        </template>
        <template v-else-if="column.key === 'resource'">
          <span style="font-family: monospace; font-size: 11.5px">
            {{ record.resourceType }}<template v-if="record.resourceId">#{{ record.resourceId }}</template>
          </span>
        </template>
        <template v-else-if="column.key === 'result'">
          <a-badge
            :status="record.result === 'SUCCESS' ? 'success' : 'error'"
            :text="record.result === 'SUCCESS' ? t('common.success') : t('common.failed')"
          />
        </template>
        <template v-else-if="column.key === 'costMs'">
          <span style="font-family: monospace; font-size: 11.5px">{{ record.costMs }}ms</span>
        </template>
      </template>

      <template #expandedRowRender="{ record }">
        <div style="display: grid; grid-template-columns: 160px 1fr; gap: 6px 14px; font-size: 12px">
          <span style="opacity: 0.45">{{ t('audit.request') }}</span>
          <span style="font-family: monospace">{{ record.endpoint }}</span>
          <span style="opacity: 0.45">{{ t('audit.params') }}</span>
          <code style="word-break: break-all; opacity: 0.75">{{ record.requestParams || '—' }}</code>
          <template v-if="record.errorMsg">
            <span style="opacity: 0.45">{{ t('audit.errorMsg') }}</span>
            <span style="color: #e5544b">{{ record.errorMsg }}</span>
          </template>
        </div>
      </template>
    </a-table>
  </div>
</template>
