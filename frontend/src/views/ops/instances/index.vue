<script setup lang="ts">
/** 运行记录：实例列表 + 节点瀑布图（对齐原型 04-trace） */
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { instanceApi, type InstanceStats, type InstanceVO, type NodeTrace } from '@/api/instance'
import { flowApi, type FlowVO } from '@/api/flow'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<InstanceVO[]>([])
const total = ref(0)
const stats = ref<InstanceStats | null>(null)
const flows = ref<FlowVO[]>([])

const filterFlow = ref<string | undefined>(undefined)
const filterStatus = ref<string | undefined>(undefined)
const filterTrigger = ref<string | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(20)

/* 详情抽屉 */
const drawerOpen = ref(false)
const detailLoading = ref(false)
const cur = ref<InstanceVO | null>(null)
const nodes = ref<NodeTrace[]>([])
const selectedSeq = ref<number | null>(null)

const columns = computed(() => [
  { title: t('inst.colId'), key: 'id', width: 150 },
  { title: t('inst.colFlow'), key: 'flow', width: 210 },
  { title: t('common.status'), key: 'status', width: 110 },
  { title: t('inst.colTrigger'), key: 'trigger', width: 110 },
  { title: t('inst.colCost'), key: 'cost', width: 100, align: 'right' as const },
  { title: t('inst.colStarted'), key: 'started', width: 180 },
  { title: t('common.actions'), key: 'action', width: 100, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    const r = await instanceApi.list({
      flowId: filterFlow.value,
      status: filterStatus.value,
      triggerType: filterTrigger.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    rows.value = r.list
    total.value = r.total
    stats.value = await instanceApi.stats(24)
  } finally {
    loading.value = false
  }
}

async function openDetail(row: InstanceVO) {
  cur.value = row
  drawerOpen.value = true
  detailLoading.value = true
  selectedSeq.value = null
  try {
    const d = await instanceApi.detail(row.id)
    cur.value = d.instance
    nodes.value = d.nodes
    if (d.nodes.length) selectedSeq.value = d.nodes[0].seq
  } finally {
    detailLoading.value = false
  }
}

/* ── 瀑布图：按开始时间求相对偏移 ─────────────────── */
const totalMs = computed(() => Math.max(1, cur.value?.costMs ?? 1))
const baseTime = computed(() => {
  if (!nodes.value.length) return 0
  return Math.min(...nodes.value.map((n) => new Date(n.startedAt).getTime()))
})
const bars = computed(() =>
  nodes.value.map((n) => {
    const off = new Date(n.startedAt).getTime() - baseTime.value
    const left = Math.max(0, Math.min(100, (off / totalMs.value) * 100))
    const width = Math.max(0.6, Math.min(100 - left, (n.costMs / totalMs.value) * 100))
    return { ...n, left, width }
  })
)
const selectedNode = computed(() => nodes.value.find((n) => n.seq === selectedSeq.value) ?? null)

const fmt = (x?: string) => (x ? x.replace('T', ' ').slice(0, 19) : '—')
const shortId = (id: string) => (id.length > 14 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id)
const pretty = (s?: string) => {
  if (!s) return '—'
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}
const statusColor = (s: string) =>
  s === 'SUCCESS' ? '#39cfbf' : s === 'FAILED' ? '#e5544b' : '#c99a3e'

onMounted(async () => {
  const f = await flowApi.list()
  flows.value = f
  await load()
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('inst.title') }}</h1>
    <p class="eeip-page-desc">{{ t('inst.desc') }}</p>

    <!-- 指标卡 -->
    <div class="cards">
      <div class="card">
        <div class="k">{{ t('inst.total') }}</div>
        <div class="v">{{ stats?.total ?? 0 }}<small>24h</small></div>
      </div>
      <div class="card">
        <div class="k">{{ t('inst.successRate') }}</div>
        <div class="v" :style="{ color: (stats?.successRate ?? 0) >= 95 ? '#39cfbf' : '#c99a3e' }">
          {{ stats?.successRate ?? 0 }}<small>%</small>
        </div>
      </div>
      <div class="card">
        <div class="k">{{ t('inst.avgCost') }}</div>
        <div class="v">{{ stats?.avgCostMs ?? 0 }}<small>ms</small></div>
      </div>
      <div class="card">
        <div class="k">{{ t('inst.failed') }}</div>
        <div class="v" :style="{ color: (stats?.failed ?? 0) > 0 ? '#e5544b' : '#5b6b79' }">
          {{ stats?.failed ?? 0 }}
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <a-space style="margin-bottom: 12px" wrap>
      <a-select v-model:value="filterFlow" :placeholder="t('inst.colFlow')" style="width: 200px"
                allow-clear @change="pageNum = 1; load()">
        <a-select-option v-for="f in flows" :key="f.id" :value="f.id">{{ f.name }}</a-select-option>
      </a-select>
      <a-select v-model:value="filterStatus" :placeholder="t('common.status')" style="width: 130px"
                allow-clear @change="pageNum = 1; load()">
        <a-select-option value="SUCCESS">SUCCESS</a-select-option>
        <a-select-option value="FAILED">FAILED</a-select-option>
        <a-select-option value="RUNNING">RUNNING</a-select-option>
      </a-select>
      <a-select v-model:value="filterTrigger" :placeholder="t('inst.colTrigger')" style="width: 140px"
                allow-clear @change="pageNum = 1; load()">
        <a-select-option value="MANUAL">MANUAL</a-select-option>
        <a-select-option value="SCHEDULE">SCHEDULE</a-select-option>
        <a-select-option value="HTTP">HTTP</a-select-option>
      </a-select>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :scroll="{ x: 1080 }"
             :pagination="{
               current: pageNum, pageSize, total, showSizeChanger: true,
               showTotal: (n: number) => t('inst.totalRows', { n })
             }"
             @change="(p: any) => { pageNum = p.current; pageSize = p.pageSize; load() }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'id'">
          <span class="mono">{{ shortId(record.id) }}</span>
        </template>
        <template v-else-if="column.key === 'flow'">
          <div class="flowname">
            <b>{{ record.flowName || record.flowCode }}</b>
            <span>{{ record.flowCode }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <span class="badge" :style="{ color: statusColor(record.status) }">
            <i :style="{ background: statusColor(record.status) }" />{{ record.status }}
          </span>
        </template>
        <template v-else-if="column.key === 'trigger'">
          <a-tag :color="record.triggerType === 'SCHEDULE' ? 'blue' : 'default'">
            {{ record.triggerType }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'cost'">
          <span class="mono">{{ record.costMs }}ms</span>
        </template>
        <template v-else-if="column.key === 'started'">
          <span class="mono dim">{{ fmt(record.startedAt) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a @click="openDetail(record)">{{ t('inst.detail') }}</a>
        </template>
      </template>
    </a-table>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="drawerOpen" :width="980" :title="t('inst.traceTitle')" placement="right">
      <a-spin :spinning="detailLoading">
        <div v-if="cur" class="tstrip">
          <div><div class="k">{{ t('inst.entry') }}</div>
            <div class="v mono">{{ cur.flowCode }}</div></div>
          <div><div class="k">{{ t('common.status') }}</div>
            <div class="v" :style="{ color: statusColor(cur.status) }">{{ cur.status }}</div></div>
          <div><div class="k">{{ t('inst.colCost') }}</div>
            <div class="v">{{ cur.costMs }} <span class="dim">ms</span></div></div>
          <div><div class="k">{{ t('inst.nodeCount') }}</div>
            <div class="v">{{ nodes.length }}</div></div>
          <div><div class="k">{{ t('inst.triggeredAt') }}</div>
            <div class="v mono">{{ fmt(cur.startedAt) }}</div></div>
        </div>

        <div class="trace-grid">
          <!-- 瀑布 -->
          <div class="panel">
            <div class="panel__head">
              <span>{{ t('inst.waterfall') }}</span>
              <span class="dim tiny">{{ t('inst.waterfallHint') }}</span>
            </div>
            <div class="panel__body">
              <div v-for="b in bars" :key="b.seq" class="wf-row" :class="{ 'is-sel': b.seq === selectedSeq }"
                   @click="selectedSeq = b.seq">
                <div class="wf-lab">
                  <i :style="{ background: b.status === 'FAILED' ? '#e5544b' : b.status === 'SUCCESS' ? '#39cfbf' : '#c99a3e' }" />
                  <span>{{ b.nodeName || b.nodeId }}</span>
                  <em>{{ b.costMs }}ms</em>
                </div>
                <div class="wf-track">
                  <span class="wf-bar"
                        :style="{ left: b.left + '%', width: b.width + '%',
                                  background: b.status === 'FAILED' ? '#e5544b' : b.status === 'SUCCESS' ? '#2f8f83' : '#8a6a34' }" />
                </div>
              </div>
              <a-empty v-if="!bars.length" :description="t('inst.noNodes')" />
            </div>
          </div>

          <!-- 选中节点 -->
          <div class="panel">
            <div class="panel__head">
              <span>{{ t('inst.selectedNode') }}</span>
              <span v-if="selectedNode" class="dim tiny">{{ selectedNode.nodeType }} · #{{ selectedNode.seq }}</span>
            </div>
            <div class="panel__body" v-if="selectedNode">
              <div class="kv"><span class="dim">{{ t('inst.nodeId') }}</span><span class="mono">{{ selectedNode.nodeId }}</span></div>
              <div class="kv"><span class="dim">{{ t('inst.colCost') }}</span><span class="mono">{{ selectedNode.costMs }}ms</span></div>
              <div class="kv" v-if="selectedNode.error"><span class="dim">{{ t('inst.error') }}</span>
                <span class="mono" style="color:#e5544b">{{ selectedNode.error }}</span></div>
              <div class="sub">{{ t('inst.input') }}</div>
              <pre class="code">{{ pretty(selectedNode.inputJson) }}</pre>
              <div class="sub">{{ t('inst.output') }}</div>
              <pre class="code">{{ pretty(selectedNode.outputJson) }}</pre>
            </div>
            <div v-else class="panel__body dim">{{ t('inst.pickNode') }}</div>
          </div>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
.card { background: #0a0f14; border: 1px solid #18222c; padding: 12px 14px; }
.card .k { font-size: 11px; color: #5b6b79; }
.card .v { font-family: ui-monospace, monospace; font-size: 22px; margin-top: 4px; color: #e6edf3; }
.card .v small { font-size: 10px; color: #5b6b79; margin-left: 3px; }
.mono { font-family: ui-monospace, monospace; font-size: 12px; }
.dim { color: #5b6b79; }
.tiny { font-size: 10.5px; }
.flowname { display: flex; flex-direction: column; }
.flowname b { font-weight: 400; font-size: 12.5px; color: #e6edf3; }
.flowname span { font-family: ui-monospace, monospace; font-size: 10px; color: #5b6b79; }
.badge { display: inline-flex; align-items: center; gap: 5px; font-size: 11.5px; }
.badge i { width: 5px; height: 5px; border-radius: 50%; }
.tstrip { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; padding: 12px 14px;
  background: #0a0f14; border: 1px solid #18222c; margin-bottom: 14px; }
.tstrip .k { font-size: 10.5px; color: #5b6b79; }
.tstrip .v { font-size: 13px; margin-top: 3px; }
.trace-grid { display: grid; grid-template-columns: 1fr 360px; gap: 14px; align-items: start; }
.panel { background: #0a0f14; border: 1px solid #18222c; }
.panel__head { display: flex; align-items: baseline; gap: 10px; justify-content: space-between;
  padding: 9px 12px; border-bottom: 1px solid #18222c; font-size: 11.5px; color: #a9b7c3; }
.panel__body { padding: 10px 12px; }
.wf-row { display: grid; grid-template-columns: 220px 1fr; gap: 10px; align-items: center;
  padding: 3px 0; cursor: pointer; }
.wf-row:hover, .wf-row.is-sel { background: rgba(242, 163, 60, .07); }
.wf-lab { display: flex; align-items: center; gap: 6px; font-size: 11.5px; color: #c7d3dd; }
.wf-lab i { width: 6px; height: 6px; flex: none; }
.wf-lab em { margin-left: auto; font-style: normal; font-family: ui-monospace, monospace;
  font-size: 10.5px; color: #5b6b79; }
.wf-track { position: relative; height: 14px; background: #0e161d; }
.wf-bar { position: absolute; top: 4px; height: 6px; }
.kv { display: flex; justify-content: space-between; font-size: 11.5px; padding: 3px 0; gap: 10px; }
.sub { margin: 10px 0 4px; font-size: 11px; color: #5b6b79; }
.code { background: #070b0f; border: 1px solid #141d26; padding: 8px 10px; margin: 0;
  font-family: ui-monospace, monospace; font-size: 10.5px; line-height: 1.55; color: #9fb0be;
  max-height: 220px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
</style>
