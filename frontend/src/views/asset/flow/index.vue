<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { flowApi, type FlowDsl, type FlowVO, type FlowValidateResult } from '@/api/flow'
import FlowGraph from './FlowGraph.vue'

const { t } = useI18n()

const loading = ref(false)
const rows = ref<FlowVO[]>([])
const keyword = ref('')

const editorOpen = ref(false)
const editorTitle = ref('')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({ code: '', name: '', triggerType: 'HTTP', description: '', changeLog: '' })
const dslText = ref('')
const dslError = ref<string | null>(null)
const validation = ref<FlowValidateResult | null>(null)

/** 解析后的 DSL，用于预览 */
const parsedDsl = computed<FlowDsl | null>(() => {
  try {
    const obj = JSON.parse(dslText.value || '{}')
    return Array.isArray(obj.nodes) ? (obj as FlowDsl) : null
  } catch {
    return null
  }
})

const columns = computed(() => [
  { title: t('flow.colCode'), dataIndex: 'code', key: 'code', width: 170 },
  { title: t('flow.colName'), dataIndex: 'name', key: 'name', width: 200 },
  { title: t('flow.colStatus'), key: 'status', width: 120 },
  { title: t('flow.colVersion'), key: 'version', width: 100 },
  { title: t('flow.colNodes'), dataIndex: 'nodeCount', key: 'nodeCount', width: 90 },
  { title: t('flow.colHash'), dataIndex: 'dslHash', key: 'dslHash', width: 130 },
  { title: t('common.actions'), key: 'action', width: 260, fixed: 'right' as const }
])

const DEFAULT_DSL = {
  schemaVersion: '1.0',
  nodes: [
    { id: 'n1', type: 'START', name: '开始', config: { description: '手工触发' }, next: 'n2' },
    {
      id: 'n2', type: 'HTTP_INVOKE', name: '调用 mock 订单接口',
      config: { connectionCode: 'mock_orders', operation: 'getOrder', timeoutMs: 5000 }, next: 'n3'
    },
    {
      id: 'n3', type: 'TRANSFORM', name: '提取字段',
      config: { mapping: { orderNo: '$.data.orderNo', amount: '$.data.amount.value' }, outputVar: 'orderReq' },
      next: 'n4'
    },
    { id: 'n4', type: 'END', name: '结束' }
  ]
}

async function load() {
  loading.value = true
  try {
    rows.value = await flowApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  editorTitle.value = t('flow.createTitle')
  Object.assign(form, { code: '', name: '', triggerType: 'HTTP', description: '', changeLog: '' })
  dslText.value = JSON.stringify(DEFAULT_DSL, null, 2)
  validation.value = null
  dslError.value = null
  editorOpen.value = true
}

async function openEdit(row: FlowVO) {
  editingId.value = row.id
  editorTitle.value = t('flow.editTitle', { name: row.name })
  Object.assign(form, {
    code: row.code, name: row.name, triggerType: row.triggerType,
    description: row.description ?? '', changeLog: ''
  })
  validation.value = null
  dslError.value = null
  const detail = await flowApi.detail(row.id)
  dslText.value = JSON.stringify(detail.dsl ?? DEFAULT_DSL, null, 2)
  editorOpen.value = true
}

function formatDsl() {
  try {
    dslText.value = JSON.stringify(JSON.parse(dslText.value), null, 2)
    dslError.value = null
  } catch (e: any) {
    dslError.value = String(e.message)
  }
}

/** 调后端校验：结构、引用、可达性、环、节点配置一次说清 */
async function runValidate() {
  let dsl: FlowDsl
  try {
    dsl = JSON.parse(dslText.value)
    dslError.value = null
  } catch (e: any) {
    dslError.value = String(e.message)
    validation.value = null
    return
  }
  validation.value = await flowApi.validate(dsl)
}

async function submit() {
  let dsl: FlowDsl
  try {
    dsl = JSON.parse(dslText.value)
  } catch (e: any) {
    message.error(t('flow.jsonInvalid') + ': ' + e.message)
    return
  }
  submitting.value = true
  try {
    const payload = { ...form, dsl }
    if (editingId.value) {
      await flowApi.update(editingId.value, payload)
      message.success(t('flow.saved'))
    } else {
      await flowApi.create(payload)
      message.success(t('flow.created'))
    }
    editorOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function publish(row: FlowVO) {
  Modal.confirm({
    title: t('flow.publishConfirm', { name: row.name }),
    content: t('flow.publishHint'),
    async onOk() {
      try {
        const r = await flowApi.publish(row.id, form.changeLog || undefined)
        message.success(t('flow.published', { v: r.version }))
        load()
      } catch { /* 错误由拦截器提示 */ }
    }
  })
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
      <a-button v-hasPerm="'asset:flow:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('flow.create') }}
      </a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 1100 }">
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
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'asset:flow:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
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

    <a-modal v-model:open="editorOpen" :title="editorTitle" :confirm-loading="submitting"
             width="1080px" :ok-text="t('common.save')" :cancel-text="t('common.cancel')" @ok="submit">
      <a-row :gutter="16" style="margin-top: 8px">
        <a-col :span="14">
          <a-form layout="vertical">
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item :label="t('flow.colCode')" required>
                  <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="demo-order-sync" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('flow.colName')" required>
                  <a-input v-model:value="form.name" placeholder="订单同步演示" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('flow.triggerType')">
                  <a-select v-model:value="form.triggerType">
                    <a-select-option value="HTTP">HTTP</a-select-option>
                    <a-select-option value="MQ">MQ</a-select-option>
                    <a-select-option value="SCHEDULE">SCHEDULE</a-select-option>
                    <a-select-option value="EVENT">EVENT</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('flow.changeLog')">
                  <a-input v-model:value="form.changeLog" :placeholder="t('flow.changeLogPlaceholder')" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="t('flow.dsl')">
              <div style="display: flex; gap: 8px; margin-bottom: 6px">
                <a-button size="small" @click="formatDsl">{{ t('flow.format') }}</a-button>
                <a-button size="small" type="primary" @click="runValidate">
                  {{ t('flow.validate') }}
                </a-button>
              </div>
              <a-textarea v-model:value="dslText" :rows="18"
                          style="font-family: monospace; font-size: 11.5px" spellcheck="false" />
            </a-form-item>
          </a-form>
        </a-col>

        <a-col :span="10">
          <div style="font-size: 12px; opacity: .55; margin-bottom: 6px">{{ t('flow.preview') }}</div>
          <FlowGraph v-if="parsedDsl" :nodes="parsedDsl.nodes" />
          <a-empty v-else :description="t('flow.previewEmpty')" />

          <template v-if="validation">
            <a-divider style="margin: 12px 0" />
            <a-alert :type="validation.valid ? 'success' : 'error'" show-icon
                     :message="validation.valid ? t('flow.validatePass') : t('flow.validateFail')" />
            <div v-if="validation.errors.length" style="margin-top: 8px">
              <div v-for="(e, i) in validation.errors" :key="'e' + i" class="flow-issue flow-issue--error">
                <code>{{ e.code }}</code> {{ e.message }}
              </div>
            </div>
            <div v-if="validation.warnings.length" style="margin-top: 8px">
              <div v-for="(w, i) in validation.warnings" :key="'w' + i" class="flow-issue flow-issue--warn">
                <code>{{ w.code }}</code> {{ w.message }}
              </div>
            </div>
          </template>
          <a-alert v-if="dslError" type="error" show-icon style="margin-top: 8px" :message="dslError" />
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>

<style scoped>
.flow-issue {
  font-size: 11.5px;
  padding: 4px 8px;
  margin-bottom: 4px;
  border-left: 2px solid;
  background: #0a0f14;
  word-break: break-all;
}
.flow-issue--error { border-color: #e5544b; color: #ffb3ad; }
.flow-issue--warn { border-color: #f2a33c; color: #ffd9a3; }
.flow-issue code { opacity: .6; margin-right: 4px; }
</style>
