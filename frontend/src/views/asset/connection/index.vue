<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { ApiOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { connectionApi, type ConnectionPayload, type ConnectionVO } from '@/api/connection'

const { t } = useI18n()
const loading = ref(false)
const testing = ref<string | null>(null)
const rows = ref<ConnectionVO[]>([])
const keyword = ref('')

const modalOpen = ref(false)
const modalTitle = ref('新建连接')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<ConnectionPayload>({
  code: '',
  name: '',
  endpoint: '',
  method: 'GET',
  authType: 'NONE',
  authConfig: '',
  headersJson: '{"User-Agent":"eeip-connector"}',
  timeoutMs: 5000,
  status: 1,
  remark: ''
})

const resultOpen = ref(false)
const result = ref<any>(null)
const resultTitle = ref('连通性测试')

const columns = computed(() => [
  { title: t('conn.colCode'), dataIndex: 'code', key: 'code', width: 150 },
  { title: t('conn.colName'), dataIndex: 'name', key: 'name', width: 160 },
  { title: t('conn.colEndpoint'), dataIndex: 'endpoint', key: 'endpoint' },
  { title: t('conn.colMethod'), dataIndex: 'method', key: 'method', width: 80 },
  { title: t('conn.colAuth'), key: 'authType', width: 110 },
  { title: t('conn.colLastCheck'), key: 'lastCheck', width: 170 },
  { title: t('common.actions'), key: 'action', width: 180, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await connectionApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  modalTitle.value = t('conn.createTitle')
  Object.assign(form, {
    code: '', name: '', endpoint: '', method: 'GET', authType: 'NONE',
    authConfig: '', headersJson: '{"User-Agent":"eeip-connector"}',
    timeoutMs: 5000, status: 1, remark: ''
  })
  modalOpen.value = true
}

function openEdit(row: ConnectionVO) {
  editingId.value = row.id
  modalTitle.value = t('conn.editTitle', { name: row.name })
  Object.assign(form, {
    code: row.code,
    name: row.name,
    endpoint: row.endpoint,
    method: row.method,
    authType: row.authType,
    authConfig: '', // 留空表示不修改凭证
    headersJson: '',
    timeoutMs: 5000,
    status: row.status,
    remark: row.remark ?? ''
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.code || !form.endpoint) {
    message.warning(t('conn.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await connectionApi.update(editingId.value, form)
      message.success(t('conn.saved'))
    } else {
      await connectionApi.create(form)
      message.success(t('conn.created'))
    }
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function showResult(title: string, r: any) {
  resultTitle.value = title
  result.value = r
  resultOpen.value = true
}

async function test(row: ConnectionVO) {
  testing.value = row.id
  try {
    const r = await connectionApi.test(row.id)
    showResult(`连通性测试 · ${row.name}`, r)
    load()
  } finally {
    testing.value = null
  }
}

async function testDraft() {
  if (!form.endpoint) {
    message.warning(t('conn.endpointRequired'))
    return
  }
  const r = await connectionApi.testDraft(form)
  showResult('预检结果（未保存）', r)
}

function remove(row: ConnectionVO) {
  Modal.confirm({
    title: t('conn.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() {
      await connectionApi.remove(row.id)
      message.success(t('conn.deleted'))
      load()
    }
  })
}

const fmt = (t?: string) => (t ? t.replace('T', ' ').slice(0, 19) : '—')

onMounted(load)
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('conn.title') }}</h1>
    <p class="eeip-page-desc">
      {{ t('conn.desc') }}
    </p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('conn.searchPlaceholder')" style="width: 220px" allow-clear
               @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'asset:connection:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('conn.create') }}
      </a-button>
    </a-space>

    <a-table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
      :scroll="{ x: 1100 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <span style="font-family: monospace; font-size: 12px">{{ record.code }}</span>
        </template>
        <template v-else-if="column.key === 'endpoint'">
          <span style="font-size: 12px">{{ record.endpoint }}</span>
        </template>
        <template v-else-if="column.key === 'authType'">
          <a-tag :color="record.authType === 'NONE' ? 'default' : 'orange'">
            {{ record.authType }}{{ record.hasAuth ? '' : '' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'lastCheck'">
          <template v-if="record.lastCheckStatus">
            <a-badge
              :status="record.lastCheckStatus === 'SUCCESS' ? 'success' : 'error'"
              :text="record.lastCheckStatus === 'SUCCESS' ? `${record.lastCheckMs}ms` : '失败'"
            />
            <div style="font-size: 10.5px; opacity: .4; font-family: monospace">
              {{ fmt(record.lastCheckAt) }}
            </div>
          </template>
          <span v-else style="opacity: .35">{{ t('common.untested') }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a
              v-hasPerm="'asset:connection:test'"
              :style="{ opacity: testing === record.id ? 0.4 : 1 }"
              @click="testing !== record.id && test(record)"
            >
              <ApiOutlined /> {{ testing === record.id ? t('common.testing') : t('common.test') }}
            </a>
            <a v-hasPerm="'asset:connection:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a v-hasPerm="'asset:connection:delete'" style="color: #e5544b"
               @click="remove(record)">{{ t('common.delete') }}</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="submitting"
      width="640px"
      @ok="submit"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('conn.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" :placeholder="t('conn.codePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('conn.colName')" required>
              <a-input v-model:value="form.name" :placeholder="t('conn.namePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="18">
            <a-form-item :label="t('conn.colEndpoint')" required>
              <a-input v-model:value="form.endpoint" :placeholder="t('conn.endpointPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :label="t('conn.colMethod')">
              <a-select v-model:value="form.method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('conn.colAuth')">
              <a-select v-model:value="form.authType">
                <a-select-option value="NONE">NONE</a-select-option>
                <a-select-option value="BEARER">BEARER</a-select-option>
                <a-select-option value="BASIC">BASIC</a-select-option>
                <a-select-option value="API_KEY">API_KEY</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('conn.timeout')">
              <a-input-number v-model:value="form.timeoutMs" :min="500" :max="60000" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24" v-if="form.authType !== 'NONE'">
            <a-form-item :label="editingId ? t('conn.authConfigEdit') : t('conn.authConfig')">
              <a-textarea v-model:value="form.authConfig" :rows="2"
                          placeholder='BEARER: {"token":"xxx"} / BASIC: {"username":"u","password":"p"}' />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('conn.headers')">
              <a-textarea v-model:value="form.headersJson" :rows="2"
                          placeholder='{"Accept":"application/json"}' />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.remark')">
              <a-input v-model:value="form.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-space>
          <a-button v-hasPerm="'asset:connection:test'" @click="testDraft">{{ t('conn.precheck') }}</a-button>
          <a-button @click="modalOpen = false">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" :loading="submitting" @click="submit">{{ t('common.save') }}</a-button>
        </a-space>
      </template>
    </a-modal>

    <a-modal v-model:open="resultOpen" :title="resultTitle" :footer="null" width="680px">
      <template v-if="result">
        <a-descriptions :column="3" size="small" bordered style="margin-bottom: 12px">
          <a-descriptions-item :label="t('conn.result')">
            <a-badge :status="result.success ? 'success' : 'error'"
                     :text="result.success ? '成功' : '失败'" />
          </a-descriptions-item>
          <a-descriptions-item :label="t('conn.statusCode')">{{ result.status ?? '—' }}</a-descriptions-item>
          <a-descriptions-item :label="t('conn.cost')">{{ result.costMs }} ms</a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="result.error" type="error" show-icon :message="result.error" style="margin-bottom: 12px" />
        <div style="font-size: 12px; opacity: .5; margin-bottom: 6px">{{ t('conn.body') }}</div>
        <pre style="background:#080c11;border:1px solid #18222c;padding:10px;font-size:11.5px;
                    max-height:260px;overflow:auto;white-space:pre-wrap;word-break:break-all">{{ result.bodySnippet || '—' }}</pre>
      </template>
    </a-modal>
  </div>
</template>
