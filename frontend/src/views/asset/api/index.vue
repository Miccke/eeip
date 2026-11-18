<script setup lang="ts">
/** API 资产：把内部系统接口登记并对外提供 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { ApiOutlined, CopyOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { openApiApi, type OpenApiPayload, type OpenApiVO } from '@/api/openApi'
import { connectionApi, type ConnectionVO } from '@/api/connection'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<OpenApiVO[]>([])
const connections = ref<ConnectionVO[]>([])
const keyword = ref('')

const modalOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<OpenApiPayload>({
  code: '', name: '', method: 'GET', path: '/', targetType: 'PROXY',
  connectionCode: '', upstreamPath: '', authType: 'API_KEY', qps: 100,
  status: 1, version: 'v1', remark: ''
})

const columns = computed(() => [
  { title: t('openApi.colMethod'), dataIndex: 'method', key: 'method', width: 80 },
  { title: t('openApi.colPath'), dataIndex: 'path', key: 'path', width: 240 },
  { title: t('openApi.colName'), dataIndex: 'name', key: 'name', width: 200 },
  { title: t('openApi.colAuth'), key: 'auth', width: 120 },
  { title: t('openApi.colUpstream'), key: 'upstream', width: 220 },
  { title: t('common.status'), key: 'status', width: 90 },
  { title: t('common.actions'), key: 'action', width: 230, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await openApiApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

async function loadConnections() {
  connections.value = await connectionApi.list()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    code: '', name: '', method: 'GET', path: '/', targetType: 'PROXY',
    connectionCode: connections.value[0]?.code ?? '', upstreamPath: '',
    authType: 'API_KEY', apiKey: '', qps: 100, status: 1, version: 'v1', remark: ''
  })
  modalOpen.value = true
}

function openEdit(row: OpenApiVO) {
  editingId.value = row.id
  Object.assign(form, {
    code: row.code, name: row.name, method: row.method, path: row.path,
    targetType: row.targetType, connectionCode: row.connectionCode ?? '',
    upstreamPath: row.upstreamPath ?? '', authType: row.authType,
    apiKey: '', qps: row.qps, status: row.status, version: row.version,
    remark: row.remark ?? ''
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.code || !form.path) {
    message.warning(t('openApi.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await openApiApi.update(editingId.value, form)
      message.success(t('common.success'))
    } else {
      const id = await openApiApi.create(form)
      const key = form.authType === 'API_KEY' ? await openApiApi.resetKey(String(id)) : null
      editingId.value = String(id)
      if (key) showKey(key)
      message.success(t('openApi.created'))
    }
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

/* 新 Key 只显示一次 */
const keyModal = ref(false)
const newKey = ref('')
function showKey(k: string) {
  newKey.value = k
  keyModal.value = true
}

async function resetKey(row: OpenApiVO) {
  Modal.confirm({
    title: t('openApi.resetConfirm', { name: row.name }),
    async onOk() {
      const k = await openApiApi.resetKey(row.id)
      showKey(k)
      load()
    }
  })
}

function copyCurl(row: OpenApiVO) {
  const curl = `curl -X ${row.method} "http://localhost:8080${row.fullPath}" \\\n  -H "X-Api-Key: <你的KEY>" \\\n  -H "Content-Type: application/json"`
  navigator.clipboard?.writeText(curl).then(() => message.success(t('common.copied')))
}

function remove(row: OpenApiVO) {
  Modal.confirm({
    title: t('openApi.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() {
      await openApiApi.remove(row.id)
      message.success(t('common.success'))
      load()
    }
  })
}

const upstreamText = (r: OpenApiVO) =>
  r.targetType === 'FLOW' ? t('openApi.targetFlow') : `${r.connectionCode || '—'}${r.upstreamPath || ''}`

onMounted(async () => {
  await Promise.all([load(), loadConnections()])
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('openApi.title') }}</h1>
    <p class="eeip-page-desc">{{ t('openApi.desc') }}</p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('openApi.searchPlaceholder')" style="width: 240px"
               allow-clear @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'asset:api:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('openApi.create') }}
      </a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 1150 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'method'">
          <a-tag :color="record.method === 'GET' ? 'blue' : record.method === 'POST' ? 'green' : 'orange'">
            {{ record.method }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'path'">
          <span style="font-family: monospace; font-size: 12px">{{ record.fullPath }}</span>
        </template>
        <template v-else-if="column.key === 'auth'">
          <a-badge :status="record.authType === 'NONE' ? 'default' : 'success'"
                   :text="record.authType === 'NONE' ? t('openApi.authNone') : `API_KEY · ${record.qps} QPS`" />
        </template>
        <template v-else-if="column.key === 'upstream'">
          <span style="font-size: 11.5px; opacity: .75">{{ upstreamText(record) }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'default'"
                   :text="record.status === 1 ? t('common.enabled') : t('common.disabled')" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'asset:api:test'" @click="copyCurl(record)"><CopyOutlined /> {{ t('openApi.copyCurl') }}</a>
            <a v-hasPerm="'asset:api:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a v-hasPerm="'asset:api:update'" @click="resetKey(record)">{{ t('openApi.resetKey') }}</a>
            <a v-hasPerm="'asset:api:delete'" style="color: #e5544b" @click="remove(record)">
              {{ t('common.delete') }}
            </a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalOpen" :title="editingId ? t('openApi.editTitle') : t('openApi.createTitle')"
             :confirm-loading="submitting" width="640px" :ok-text="t('common.save')" :cancel-text="t('common.cancel')"
             @ok="submit">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('openApi.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="order-query" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('openApi.colName')" required>
              <a-input v-model:value="form.name" placeholder="订单查询（对外）" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :label="t('openApi.colMethod')">
              <a-select v-model:value="form.method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="18">
            <a-form-item :label="t('openApi.colPath')" required>
              <a-input v-model:value="form.path" placeholder="/orders/{id}">
                <template #addonBefore>/api/open</template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('openApi.upstreamConnection')">
              <a-select v-model:value="form.connectionCode" allow-clear :placeholder="t('openApi.pickConnection')">
                <a-select-option v-for="c in connections" :key="c.id" :value="c.code">
                  {{ c.name }} · {{ c.endpoint }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('openApi.upstreamPath')">
              <a-input v-model:value="form.upstreamPath" placeholder="/orders/{id}" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('openApi.colAuth')">
              <a-select v-model:value="form.authType">
                <a-select-option value="API_KEY">API_KEY</a-select-option>
                <a-select-option value="NONE">NONE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('openApi.qps')">
              <a-input-number v-model:value="form.qps" :min="0" :max="100000" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.remark')">
              <a-input v-model:value="form.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="keyModal" :title="t('openApi.keyTitle')" :footer="null" width="520px">
      <a-alert type="warning" show-icon :message="t('openApi.keyWarn')" style="margin-bottom: 12px" />
      <pre style="background:#080c11;border:1px solid #18222c;padding:10px;font-size:12px;word-break:break-all">{{ newKey }}</pre>
      <div style="margin-top: 12px; text-align: right">
        <a-button type="primary" @click="keyModal = false">{{ t('openApi.savedIt') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>
