<script setup lang="ts">
/**
 * API 资产（1:1 对齐原型 03-api-catalog.html）
 * 布局：页头 + 筛选条 + 主表格 + 右侧三块面板（资产构成 / 规范校验 / 待办）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { apiLedgerApi, type ApiPayload, type ApiVO } from '@/api/apiLedger'
import { systemApi, type SystemVO } from '@/api/system'
import { domainApi, type Domain } from '@/api/domain'
import { connectionApi, type ConnectionVO } from '@/api/connection'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<ApiVO[]>([])
const systems = ref<SystemVO[]>([])
const domains = ref<Domain[]>([])
const connections = ref<ConnectionVO[]>([])

const keyword = ref('')
const filterDomain = ref<string | undefined>(undefined)
const filterSla = ref<string | undefined>(undefined)
const filterStatus = ref<string | undefined>(undefined)

const modalOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<ApiPayload & { slaLevel?: string; targetType?: string; flowCode?: string; protocol?: string }>({
  systemCode: '', code: '', name: '', method: 'GET', path: '/', connectorCodes: [],
  changeLog: '', description: '', slaLevel: 'L2', targetType: 'CONNECTOR', flowCode: '', protocol: 'HTTP'
})

/* ── 列表 ───────────────────────────────────────────── */
async function load() {
  loading.value = true
  try {
    rows.value = await apiLedgerApi.list(undefined, keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => rows.value.filter((r) =>
  (!filterDomain.value || r.domainCode === filterDomain.value) &&
  (!filterSla.value || (r as any).slaLevel === filterSla.value) &&
  (!filterStatus.value || r.status === filterStatus.value)))

const columns = computed(() => [
  { title: t('ledger.colApi'), key: 'api', width: 260 },
  { title: t('ledger.colProvider'), key: 'provider', width: 130 },
  { title: t('ledger.colVersion'), key: 'version', width: 90 },
  { title: t('ledger.sla'), key: 'sla', width: 80 },
  { title: t('ledger.qps'), key: 'qps', width: 90, align: 'right' as const },
  { title: t('ledger.calls'), key: 'calls', width: 110, align: 'right' as const },
  { title: t('ledger.errRate'), key: 'err', width: 90, align: 'right' as const },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('ledger.owner'), key: 'owner', width: 100 },
  { title: t('common.actions'), key: 'action', width: 200, fixed: 'right' as const }
])

const stat = computed(() => {
  const all = rows.value.length
  const published = rows.value.filter((r) => r.status === 'PUBLISHED').length
  const draft = rows.value.filter((r) => r.status === 'DRAFT').length
  const offline = rows.value.filter((r) => r.status === 'OFFLINE').length
  return { all, published, draft, offline }
})

/** 规范校验：基于真实字段的轻量规则 */
const checks = computed(() => {
  const noConn = rows.value.filter((r) => !(r.connectorCodes || []).length).length
  const noSla = rows.value.filter((r) => !(r as any).slaLevel).length
  const notPublished = rows.value.filter((r) => r.status !== 'PUBLISHED').length
  const pass = rows.value.length ? Math.round(((rows.value.length - noConn) / rows.value.length) * 100) : 100
  return { noConn, noSla, notPublished, pass }
})

const donutStyle = computed(() => {
  const { all, published, draft, offline } = stat.value
  if (!all) return { background: '#22303C' }
  const p1 = (published / all) * 100
  const p2 = p1 + (draft / all) * 100
  const p3 = p2 + (offline / all) * 100
  return {
    background: `conic-gradient(#39cfbf 0 ${p1}%, #c99a3e ${p1}% ${p2}%, #8a6fd4 ${p2}% ${p3}%, #22303C ${p3}% 100%)`
  }
})

/* ── 编辑 ───────────────────────────────────────────── */
const systemConnectors = computed(() =>
  connections.value.filter((c) => !(c as any).systemCode || (c as any).systemCode === form.systemCode))

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    systemCode: systems.value[0]?.code ?? '', code: '', name: '', method: 'GET', path: '/',
    connectorCodes: [], changeLog: '', description: '',
    slaLevel: 'L2', targetType: 'CONNECTOR', flowCode: '', protocol: 'HTTP'
  })
  modalOpen.value = true
}

function openEdit(row: ApiVO) {
  editingId.value = row.id
  Object.assign(form, {
    systemCode: row.systemCode, code: row.code, name: row.name, method: row.method ?? 'GET',
    path: row.path ?? '/', connectorCodes: [...(row.connectorCodes ?? [])],
    changeLog: '', description: row.description ?? '',
    slaLevel: (row as any).slaLevel ?? 'L2', targetType: (row as any).targetType ?? 'CONNECTOR',
    flowCode: (row as any).flowCode ?? '', protocol: (row as any).protocol ?? 'HTTP'
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.systemCode || !form.code || !form.path) {
    message.warning(t('ledger.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) await apiLedgerApi.update(editingId.value, form)
    else await apiLedgerApi.create(form)
    message.success(t('common.success'))
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function publish(row: ApiVO) {
  try {
    const r = await apiLedgerApi.publish(row.id)
    message.success(t('ledger.published', { path: r.publicPath }))
    load()
  } catch { /* 拦截器提示 */ }
}

async function offline(row: ApiVO) {
  await apiLedgerApi.offline(row.id)
  message.success(t('common.success'))
  load()
}

function remove(row: ApiVO) {
  Modal.confirm({
    title: t('ledger.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() { await apiLedgerApi.remove(row.id); message.success(t('common.success')); load() }
  })
}

const statusBadge = (s: string) =>
  s === 'PUBLISHED' ? { color: '#39cfbf', text: t('ledger.statusPUBLISHED') }
  : s === 'OFFLINE' ? { color: '#5b6b79', text: t('ledger.statusOFFLINE') }
  : { color: '#c99a3e', text: t('ledger.statusDRAFT') }

const batchValidate = () => message.info(t('ledger.batchTip', { n: checks.value.noConn }))
const exportLedger = () => message.info(t('ledger.exportTip'))

onMounted(async () => {
  const [s, d, c] = await Promise.all([systemApi.list(), domainApi.list(), connectionApi.list()])
  systems.value = s
  domains.value = d
  connections.value = c
  await load()
})
</script>

<template>
  <div class="api-page">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h1 class="eeip-page-title">{{ t('ledger.title') }}</h1>
        <p class="eeip-page-desc">{{ t('ledger.subtitle', { n: stat.all }) }}</p>
      </div>
      <div class="page-head__actions">
        <a-button size="small" @click="batchValidate">{{ t('ledger.batchValidate') }}</a-button>
        <a-button size="small" @click="exportLedger">{{ t('ledger.export') }}</a-button>
        <a-button v-hasPerm="'asset:api:create'" size="small" type="primary" @click="openCreate">
          <PlusOutlined /> {{ t('ledger.create') }}
        </a-button>
      </div>
    </div>

    <!-- 筛选条 -->
    <div class="filters">
      <a-input v-model:value="keyword" :placeholder="t('ledger.searchPlaceholder')" style="width: 240px"
               allow-clear @press-enter="load" />
      <a-select v-model:value="filterDomain" :placeholder="t('ledger.domain')" style="width: 150px"
                allow-clear>
        <a-select-option v-for="d in domains" :key="d.id" :value="d.code">{{ d.name }}</a-select-option>
      </a-select>
      <a-select v-model:value="filterSla" :placeholder="t('ledger.sla')" style="width: 120px" allow-clear>
        <a-select-option value="L1">L1</a-select-option>
        <a-select-option value="L2">L2</a-select-option>
        <a-select-option value="L3">L3</a-select-option>
      </a-select>
      <a-select v-model:value="filterStatus" :placeholder="t('common.status')" style="width: 130px" allow-clear>
        <a-select-option value="PUBLISHED">{{ t('ledger.statusPUBLISHED') }}</a-select-option>
        <a-select-option value="DRAFT">{{ t('ledger.statusDRAFT') }}</a-select-option>
        <a-select-option value="OFFLINE">{{ t('ledger.statusOFFLINE') }}</a-select-option>
      </a-select>
      <a-button size="small" @click="load"><ReloadOutlined /></a-button>
      <span class="filters__count">{{ t('ledger.total', { n: filtered.length }) }}</span>
    </div>

    <!-- 主体：表格 + 右侧面板 -->
    <div class="api-grid">
      <div class="api-main">
        <a-table :columns="columns" :data-source="filtered" :loading="loading" row-key="id" size="middle"
                 :pagination="false" :scroll="{ x: 1300 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'api'">
              <div class="apiname">
                <b>{{ record.name }}</b>
                <span>{{ (record as any).apiNo || '—' }} · {{ record.method }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'provider'">
              <span style="font-size: 12px">{{ record.systemCode }}</span>
            </template>
            <template v-else-if="column.key === 'version'">
              <span class="mono">{{ record.version || '—' }}</span>
            </template>
            <template v-else-if="column.key === 'sla'">
              <a-tag :color="(record as any).slaLevel === 'L1' ? 'green' : 'default'">
                {{ (record as any).slaLevel || 'L2' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'qps'">
              <span class="mono">{{ record.status === 'PUBLISHED' ? ((record as any).qps ?? 0) : '—' }}</span>
            </template>
            <template v-else-if="column.key === 'calls'">
              <span class="mono dim">{{ t('ledger.noMetrics') }}</span>
            </template>
            <template v-else-if="column.key === 'err'">
              <span class="mono dim">{{ t('ledger.noMetrics') }}</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <span class="badge" :style="{ color: statusBadge(record.status).color }">
                <i :style="{ background: statusBadge(record.status).color }" />{{ statusBadge(record.status).text }}
              </span>
            </template>
            <template v-else-if="column.key === 'owner'">
              <span class="mono dim">—</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a v-hasPerm="'asset:api:publish'" @click="publish(record)">{{ t('ledger.publish') }}</a>
                <a v-if="record.status === 'PUBLISHED'" v-hasPerm="'asset:api:offline'"
                   @click="offline(record)">{{ t('ledger.offline') }}</a>
                <a v-hasPerm="'asset:api:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
                <a v-hasPerm="'asset:api:delete'" style="color: #e5544b" @click="remove(record)">
                  {{ t('common.delete') }}
                </a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 右侧面板 -->
      <aside class="api-side">
        <div class="panel">
          <div class="panel__head">{{ t('ledger.composition') }}</div>
          <div class="panel__body">
            <div class="donut" :style="donutStyle">
              <b>{{ stat.all }}<small>{{ t('ledger.apis') }}</small></b>
            </div>
            <div class="legend">
              <span><i style="background:#39cfbf" />{{ t('ledger.statusPUBLISHED') }}<em>{{ stat.published }}</em></span>
              <span><i style="background:#c99a3e" />{{ t('ledger.statusDRAFT') }}<em>{{ stat.draft }}</em></span>
              <span><i style="background:#8a6fd4" />{{ t('ledger.statusOFFLINE') }}<em>{{ stat.offline }}</em></span>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel__head">{{ t('ledger.spec') }}</div>
          <div class="panel__body">
            <div class="spread" style="margin-bottom: 8px">
              <span class="dim">{{ t('ledger.passRate') }}</span>
              <span class="mono" style="font-size: 15px; color: #39cfbf">{{ checks.pass }}%</span>
            </div>
            <div class="meter"><i :style="{ width: checks.pass + '%' }" /></div>
            <div class="stack" style="margin-top: 12px">
              <div class="spread"><span class="dim">{{ t('ledger.ruleNoConn') }}</span>
                <span class="mono" :style="{ color: checks.noConn ? '#e5544b' : '#5b6b79' }">{{ checks.noConn }}</span>
              </div>
              <div class="spread"><span class="dim">{{ t('ledger.ruleNoSla') }}</span>
                <span class="mono" :style="{ color: checks.noSla ? '#c99a3e' : '#5b6b79' }">{{ checks.noSla }}</span>
              </div>
              <div class="spread"><span class="dim">{{ t('ledger.ruleNotPublished') }}</span>
                <span class="mono" :style="{ color: checks.notPublished ? '#c99a3e' : '#5b6b79' }">{{ checks.notPublished }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel__head">{{ t('ledger.todo') }}</div>
          <div class="panel__body">
            <div class="todo">
              <span class="badge-num">{{ checks.noConn }}</span>
              <div><b>{{ t('ledger.todoBind') }}</b><span>{{ t('ledger.todoBindDesc') }}</span></div>
            </div>
            <div class="todo">
              <span class="badge-num">{{ checks.notPublished }}</span>
              <div><b>{{ t('ledger.todoPublish') }}</b><span>{{ t('ledger.todoPublishDesc') }}</span></div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 编辑弹窗 -->
    <a-modal v-model:open="modalOpen" :title="editingId ? t('ledger.editTitle') : t('ledger.createTitle')"
             :confirm-loading="submitting" width="700px" :ok-text="t('common.save')"
             :cancel-text="t('common.cancel')" @ok="submit">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('ledger.colSystem')" required>
              <a-select v-model:value="form.systemCode" :disabled="!!editingId">
                <a-select-option v-for="s in systems" :key="s.id" :value="s.code">
                  {{ s.code }} · {{ s.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('ledger.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="query-order" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('ledger.colName')" required>
              <a-input v-model:value="form.name" placeholder="查询订单" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('ledger.protocol')">
              <a-select v-model:value="form.protocol">
                <a-select-option value="HTTP">HTTP</a-select-option>
                <a-select-option value="SOAP">SOAP</a-select-option>
                <a-select-option value="JDBC">JDBC</a-select-option>
                <a-select-option value="MQ">MQ</a-select-option>
                <a-select-option value="FILE">FILE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('ledger.method')">
              <a-select v-model:value="form.method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('ledger.sla')">
              <a-select v-model:value="form.slaLevel">
                <a-select-option value="L1">L1 {{ t('ledger.slaL1') }}</a-select-option>
                <a-select-option value="L2">L2 {{ t('ledger.slaL2') }}</a-select-option>
                <a-select-option value="L3">L3 {{ t('ledger.slaL3') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('ledger.upstreamPath')" required>
              <a-input v-model:value="form.path" placeholder="/orders/{id}" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('ledger.impl')">
              <a-radio-group v-model:value="form.targetType">
                <a-radio value="CONNECTOR">{{ t('ledger.implConnector') }}</a-radio>
                <a-radio value="FLOW">{{ t('ledger.implFlow') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="form.targetType === 'FLOW'">
            <a-form-item :label="t('ledger.flowCode')">
              <a-input v-model:value="form.flowCode" placeholder="demo-run-order" />
            </a-form-item>
          </a-col>
          <a-col :span="24" v-if="form.targetType === 'CONNECTOR'">
            <a-form-item :label="t('ledger.bindConnectors')">
              <a-select v-model:value="form.connectorCodes" mode="multiple" :placeholder="t('ledger.bindHint')">
                <a-select-option v-for="c in systemConnectors" :key="c.id" :value="c.code">
                  {{ c.name }} · {{ c.endpoint }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.remark')">
              <a-input v-model:value="form.description" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert type="info" show-icon
                 :message="t('ledger.pathHint', { p: `/api/open/v1/${form.systemCode || '{system}'}/${form.code || '{code}'}` })" />
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.page-head__actions { display: flex; gap: 8px; }
.filters { display: flex; gap: 8px; align-items: center; margin: 12px 0; flex-wrap: wrap; }
.filters__count { margin-left: auto; font-size: 11px; color: #5b6b79; }
.api-grid { display: grid; grid-template-columns: 1fr 296px; gap: 14px; }
.api-main { min-width: 0; }
.apiname { display: flex; flex-direction: column; gap: 2px; }
.apiname b { font-weight: 400; color: #e6edf3; font-size: 12.5px; }
.apiname span { font-family: ui-monospace, monospace; font-size: 10px; color: #5b6b79; }
.mono { font-family: ui-monospace, monospace; font-size: 12px; }
.dim { color: #5b6b79; }
.badge { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; }
.badge i { width: 5px; height: 5px; border-radius: 50%; }
.api-side { display: flex; flex-direction: column; gap: 14px; }
.panel { background: #0a0f14; border: 1px solid #18222c; }
.panel__head { padding: 9px 12px; border-bottom: 1px solid #18222c; font-size: 11.5px; color: #a9b7c3; }
.panel__body { padding: 12px; }
.donut {
  width: 112px; height: 112px; border-radius: 50%; margin: 4px auto 12px;
  position: relative; display: grid; place-items: center;
}
.donut::after { content: ""; position: absolute; inset: 12px; border-radius: 50%; background: #0a0f14; }
.donut b { position: relative; z-index: 1; font-family: ui-monospace, monospace; font-size: 21px; font-weight: 500; text-align: center; }
.donut b small { display: block; font-size: 9px; color: #5b6b79; letter-spacing: .1em; font-weight: 400; }
.legend { display: flex; flex-direction: column; gap: 5px; font-size: 11px; color: #a9b7c3; }
.legend span { display: flex; align-items: center; gap: 6px; }
.legend i { width: 6px; height: 6px; }
.legend em { margin-left: auto; font-style: normal; font-family: ui-monospace, monospace; color: #5b6b79; }
.spread { display: flex; justify-content: space-between; align-items: baseline; }
.meter { height: 4px; background: #18222c; }
.meter i { display: block; height: 100%; background: #39cfbf; }
.stack { display: flex; flex-direction: column; gap: 6px; font-size: 11.5px; }
.todo { display: flex; gap: 9px; padding: 8px 0; border-bottom: 1px solid rgba(24,34,44,.6); font-size: 11.5px; }
.todo:last-child { border-bottom: 0; }
.todo b { font-weight: 400; color: #e6edf3; display: block; }
.todo span { color: #5b6b79; font-size: 10.5px; }
.badge-num {
  display: grid; place-items: center; min-width: 18px; height: 18px; padding: 0 4px;
  border: 1px solid #3a3222; color: #c99a3e; font-family: ui-monospace, monospace; font-size: 10px;
}
</style>
