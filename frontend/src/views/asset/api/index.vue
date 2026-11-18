<script setup lang="ts">
/** 接口台账：系统下有哪些接口、契约版本、绑定哪些连接器；发布后自动生成对外地址 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { apiLedgerApi, type ApiPayload, type ApiVO } from '@/api/apiLedger'
import { systemApi, type SystemVO } from '@/api/system'
import { connectionApi, type ConnectionVO } from '@/api/connection'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<ApiVO[]>([])
const systems = ref<SystemVO[]>([])
const connections = ref<ConnectionVO[]>([])
const filterSystem = ref<string | undefined>(undefined)
const keyword = ref('')

const modalOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<ApiPayload>({
  systemCode: '', code: '', name: '', method: 'GET', path: '/',
  connectorCodes: [], changeLog: '', description: ''
})

const columns = computed(() => [
  { title: t('ledger.colSystem'), dataIndex: 'systemCode', key: 'systemCode', width: 140 },
  { title: t('ledger.colCode'), dataIndex: 'code', key: 'code', width: 160 },
  { title: t('ledger.colName'), dataIndex: 'name', key: 'name', width: 160 },
  { title: t('ledger.colVersion'), key: 'version', width: 100 },
  { title: t('ledger.colContract'), key: 'contract', width: 240 },
  { title: t('ledger.colPublic'), key: 'public', width: 260 },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('common.actions'), key: 'action', width: 220, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await apiLedgerApi.list(filterSystem.value, keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    systemCode: systems.value[0]?.code ?? '', code: '', name: '', method: 'GET',
    path: '/', connectorCodes: [], changeLog: '', description: ''
  })
  modalOpen.value = true
}

function openEdit(row: ApiVO) {
  editingId.value = row.id
  Object.assign(form, {
    systemCode: row.systemCode, code: row.code, name: row.name, method: row.method ?? 'GET',
    path: row.path ?? '/', connectorCodes: [...(row.connectorCodes ?? [])],
    changeLog: '', description: row.description ?? ''
  })
  modalOpen.value = true
}

/** 当前系统下可选的连接器 */
const systemConnectors = computed(() =>
  connections.value.filter((c) => (c as any).systemCode === form.systemCode || !(c as any).systemCode))

async function submit() {
  if (!form.systemCode || !form.code || !form.path) {
    message.warning(t('ledger.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await apiLedgerApi.update(editingId.value, form)
      message.success(t('common.success'))
    } else {
      await apiLedgerApi.create(form)
      message.success(t('ledger.created'))
    }
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
    async onOk() {
      await apiLedgerApi.remove(row.id)
      message.success(t('common.success'))
      load()
    }
  })
}

const statusColor = (s: string) =>
  s === 'PUBLISHED' ? 'success' : s === 'OFFLINE' ? 'default' : 'warning'

onMounted(async () => {
  const [s, c] = await Promise.all([systemApi.list(), connectionApi.list()])
  systems.value = s
  connections.value = c
  await load()
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('ledger.title') }}</h1>
    <p class="eeip-page-desc">{{ t('ledger.desc') }}</p>

    <a-space style="margin-bottom: 12px">
      <a-select v-model:value="filterSystem" :placeholder="t('ledger.colSystem')" style="width: 180px"
                allow-clear @change="load">
        <a-select-option v-for="s in systems" :key="s.id" :value="s.code">
          {{ s.code }} · {{ s.name }}
        </a-select-option>
      </a-select>
      <a-input v-model:value="keyword" :placeholder="t('ledger.searchPlaceholder')" style="width: 200px"
               allow-clear @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'asset:api:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('ledger.create') }}
      </a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 1300 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'systemCode'">
          <a-tag color="blue">{{ record.systemCode }}</a-tag>
        </template>
        <template v-else-if="column.key === 'code'">
          <span style="font-family: monospace; font-size: 12px">{{ record.code }}</span>
        </template>
        <template v-else-if="column.key === 'version'">
          <a-tag v-if="record.version" color="orange">v{{ record.version }}</a-tag>
          <span v-else style="opacity: .4">—</span>
        </template>
        <template v-else-if="column.key === 'contract'">
          <span style="font-family: monospace; font-size: 11px">
            {{ record.method }} {{ record.path }}
          </span>
          <div style="font-size: 10.5px; opacity: .55">
            {{ t('ledger.connectors') }}: {{ (record.connectorCodes || []).join(', ') || '—' }}
          </div>
        </template>
        <template v-else-if="column.key === 'public'">
          <span v-if="record.status === 'PUBLISHED'" style="font-family: monospace; font-size: 11px; color: #39cfbf">
            {{ record.publicPath }}
          </span>
          <span v-else style="font-size: 11px; opacity: .45">{{ t('ledger.notPublished') }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="statusColor(record.status)" :text="t('ledger.status' + record.status)" />
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

    <a-modal v-model:open="modalOpen" :title="editingId ? t('ledger.editTitle') : t('ledger.createTitle')"
             :confirm-loading="submitting" width="660px" :ok-text="t('common.save')"
             :cancel-text="t('common.cancel')" @ok="submit">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('ledger.colSystem')" required>
              <a-select v-model:value="form.systemCode" :disabled="!!editingId">
                <a-select-option v-for="s in systems" :key="s.id" :value="s.code">
                  {{ s.code }} · {{ s.name }}（{{ s.env }}）
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
            <a-form-item :label="t('ledger.method')">
              <a-select v-model:value="form.method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item :label="t('ledger.upstreamPath')" required>
              <a-input v-model:value="form.path" placeholder="/orders/{id}" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
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
        <a-alert type="info" show-icon :message="t('ledger.pathHint', { p: `/api/open/v1/${form.systemCode || '{system}'}/${form.code || '{code}'}` })" />
      </a-form>
    </a-modal>
  </div>
</template>
