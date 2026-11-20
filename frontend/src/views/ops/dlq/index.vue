<script setup lang="ts">
/** 死信处置：失败实例的重投与丢弃 */
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { dlqApi, type DeadLetter, type DlqStats } from '@/api/dlq'
import { flowApi, type FlowVO } from '@/api/flow'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<DeadLetter[]>([])
const total = ref(0)
const stats = ref<DlqStats | null>(null)
const flows = ref<FlowVO[]>([])
const filterStatus = ref<string | undefined>('PENDING')
const filterFlow = ref<string | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(20)

const detailOpen = ref(false)
const cur = ref<DeadLetter | null>(null)

const columns = computed(() => [
  { title: t('dlq.colFlow'), key: 'flow', width: 200 },
  { title: t('dlq.colNode'), key: 'node', width: 150 },
  { title: t('dlq.colError'), key: 'error', width: 240 },
  { title: t('dlq.colAttempts'), key: 'attempts', width: 100, align: 'right' as const },
  { title: t('common.status'), key: 'status', width: 110 },
  { title: t('dlq.colCreated'), key: 'created', width: 180 },
  { title: t('common.actions'), key: 'action', width: 200, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    const r = await dlqApi.list({
      status: filterStatus.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    rows.value = r.list
    total.value = r.total
    stats.value = await dlqApi.stats()
  } finally {
    loading.value = false
  }
}

function doRetry(row: DeadLetter) {
  Modal.confirm({
    title: t('dlq.retryConfirm'),
    content: t('dlq.retryHint'),
    okType: 'danger',
    async onOk() {
      const r = await dlqApi.retry(row.id)
      message.success(t('dlq.retriedMsg', { id: r.instanceId, status: r.status }))
      load()
    }
  })
}

function doDiscard(row: DeadLetter) {
  Modal.confirm({
    title: t('dlq.discardConfirm'),
    okType: 'danger',
    async onOk() {
      await dlqApi.discard(row.id, t('dlq.discardRemark'))
      message.success(t('common.success'))
      load()
    }
  })
}

function openDetail(row: DeadLetter) {
  cur.value = row
  detailOpen.value = true
}

const statusColor = (s: string) =>
  s === 'PENDING' ? '#e5544b' : s === 'RETRIED' ? '#39cfbf' : '#5b6b79'
const statusText = (s: string) =>
  s === 'PENDING' ? t('dlq.stPending') : s === 'RETRIED' ? t('dlq.stRetried') : t('dlq.stDiscarded')
const fmt = (x?: string) => (x ? x.replace('T', ' ').slice(0, 19) : '—')
const pretty = (s?: string) => {
  if (!s) return '—'
  try { return JSON.stringify(JSON.parse(s), null, 2) } catch { return s }
}

onMounted(async () => {
  flows.value = await flowApi.list()
  await load()
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('dlq.title') }}</h1>
    <p class="eeip-page-desc">{{ t('dlq.desc') }}</p>

    <div class="cards">
      <div class="card"><div class="k">{{ t('dlq.pending') }}</div>
        <div class="v" style="color:#e5544b">{{ stats?.pending ?? 0 }}</div></div>
      <div class="card"><div class="k">{{ t('dlq.retried') }}</div>
        <div class="v" style="color:#39cfbf">{{ stats?.retried ?? 0 }}</div></div>
      <div class="card"><div class="k">{{ t('dlq.discarded') }}</div>
        <div class="v" style="color:#5b6b79">{{ stats?.discarded ?? 0 }}</div></div>
      <div class="card"><div class="k">{{ t('dlq.all') }}</div>
        <div class="v">{{ stats?.total ?? 0 }}</div></div>
    </div>

    <a-space style="margin-bottom: 12px">
      <a-select v-model:value="filterStatus" :placeholder="t('common.status')" style="width: 150px"
                allow-clear @change="pageNum = 1; load()">
        <a-select-option value="PENDING">{{ t('dlq.stPending') }}</a-select-option>
        <a-select-option value="RETRIED">{{ t('dlq.stRetried') }}</a-select-option>
        <a-select-option value="DISCARDED">{{ t('dlq.stDiscarded') }}</a-select-option>
      </a-select>
      <a-select v-model:value="filterFlow" :placeholder="t('dlq.colFlow')" style="width: 200px"
                allow-clear disabled>
        <a-select-option v-for="f in flows" :key="f.id" :value="f.code">{{ f.name }}</a-select-option>
      </a-select>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :scroll="{ x: 1180 }"
             :pagination="{ current: pageNum, pageSize, total, showSizeChanger: true,
                            showTotal: (n: number) => t('inst.totalRows', { n }) }"
             @change="(p: any) => { pageNum = p.current; pageSize = p.pageSize; load() }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'flow'">
          <div class="flowcell">
            <b>{{ record.flowName || record.flowCode }}</b>
            <span>{{ record.flowCode }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'node'">
          <div class="flowcell">
            <b>{{ record.nodeName || record.nodeId || '—' }}</b>
            <span>{{ record.nodeType }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'error'">
          <span class="mono" style="color:#e5544b">{{ record.error || '—' }}</span>
        </template>
        <template v-else-if="column.key === 'attempts'">
          <span class="mono">{{ record.attempts }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <span class="badge" :style="{ color: statusColor(record.status) }">
            <i :style="{ background: statusColor(record.status) }" />{{ statusText(record.status) }}
          </span>
        </template>
        <template v-else-if="column.key === 'created'">
          <span class="mono dim">{{ fmt(record.createdAt) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-if="record.status === 'PENDING'" v-hasPerm="'ops:dlq:retry'"
               style="color:#f2a33c" @click="doRetry(record)"><RedoOutlined /> {{ t('dlq.retry') }}</a>
            <a v-if="record.status === 'PENDING'" v-hasPerm="'ops:dlq:retry'"
               style="color:#5b6b79" @click="doDiscard(record)">{{ t('dlq.discard') }}</a>
            <a @click="openDetail(record)">{{ t('inst.detail') }}</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="detailOpen" :title="t('dlq.detailTitle')" :footer="null" width="720px">
      <div v-if="cur">
        <div class="kv"><span class="dim">{{ t('dlq.colFlow') }}</span>
          <span>{{ cur.flowName || cur.flowCode }}</span></div>
        <div class="kv"><span class="dim">{{ t('dlq.colNode') }}</span>
          <span class="mono">{{ cur.nodeId }} · {{ cur.nodeType }}</span></div>
        <div class="kv"><span class="dim">{{ t('dlq.colError') }}</span>
          <span class="mono" style="color:#e5544b">{{ cur.error }}</span></div>
        <div class="kv"><span class="dim">{{ t('dlq.colAttempts') }}</span>
          <span class="mono">{{ cur.attempts }}</span></div>
        <div class="kv"><span class="dim">{{ t('dlq.instance') }}</span>
          <span class="mono">{{ cur.instanceId }}</span></div>
        <div class="kv" v-if="cur.retriedInstance"><span class="dim">{{ t('dlq.newInstance') }}</span>
          <span class="mono">{{ cur.retriedInstance }}</span></div>
        <div class="kv" v-if="cur.handledBy"><span class="dim">{{ t('dlq.handledBy') }}</span>
          <span class="mono">{{ cur.handledBy }} · {{ fmt(cur.handledAt) }}</span></div>
        <div class="sub">{{ t('dlq.payload') }}</div>
        <pre class="code">{{ pretty(cur.inputJson) }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
.card { background: #0a0f14; border: 1px solid #18222c; padding: 12px 14px; }
.card .k { font-size: 11px; color: #5b6b79; }
.card .v { font-family: ui-monospace, monospace; font-size: 22px; margin-top: 4px; color: #e6edf3; }
.mono { font-family: ui-monospace, monospace; font-size: 12px; }
.dim { color: #5b6b79; }
.flowcell { display: flex; flex-direction: column; }
.flowcell b { font-weight: 400; font-size: 12.5px; color: #e6edf3; }
.flowcell span { font-family: ui-monospace, monospace; font-size: 10px; color: #5b6b79; }
.badge { display: inline-flex; align-items: center; gap: 5px; font-size: 11.5px; }
.badge i { width: 5px; height: 5px; border-radius: 50%; }
.kv { display: flex; justify-content: space-between; gap: 12px; font-size: 12px; padding: 4px 0; }
.sub { margin: 12px 0 4px; font-size: 11px; color: #5b6b79; }
.code { background: #070b0f; border: 1px solid #141d26; padding: 10px; margin: 0;
  font-family: ui-monospace, monospace; font-size: 11px; line-height: 1.6; color: #9fb0be;
  max-height: 260px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
</style>
