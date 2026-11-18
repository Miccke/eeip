<script setup lang="ts">
/** 系统注册：生产方系统台账，连接器与接口的归属 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { systemApi, type SystemPayload, type SystemVO } from '@/api/system'
import { domainApi, type Domain } from '@/api/domain'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<SystemVO[]>([])
const keyword = ref('')
const domains = ref<Domain[]>([])

const modalOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<SystemPayload & { domainCode?: string }>({
  code: '', name: '', env: 'PROD', protocol: 'HTTP', baseUrl: '',
  owner: '', ownerPhone: '', status: 1, remark: '', domainCode: 'IT'
})

const columns = computed(() => [
  { title: t('sys.colCode'), dataIndex: 'code', key: 'code', width: 160 },
  { title: t('sys.colName'), dataIndex: 'name', key: 'name', width: 180 },
  { title: t('sys.colDomain'), key: 'domainCode', width: 110 },
  { title: t('sys.colEnv'), key: 'env', width: 100 },
  { title: t('sys.colProtocol'), key: 'protocol', width: 100 },
  { title: t('sys.colOwner'), dataIndex: 'owner', key: 'owner', width: 110 },
  { title: t('sys.colCounts'), key: 'counts', width: 160 },
  { title: t('common.status'), key: 'status', width: 90 },
  { title: t('common.actions'), key: 'action', width: 140, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await systemApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', env: 'PROD', protocol: 'HTTP', baseUrl: '', owner: '', ownerPhone: '', status: 1, remark: '', domainCode: 'IT' })
  modalOpen.value = true
}

function openEdit(row: SystemVO) {
  editingId.value = row.id
  Object.assign(form, {
    code: row.code, name: row.name, env: row.env, protocol: row.protocol,
    baseUrl: row.baseUrl ?? '', owner: row.owner ?? '', ownerPhone: row.ownerPhone ?? '',
    status: row.status, remark: row.remark ?? '', domainCode: (row as any).domainCode ?? 'IT'
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.code || !form.name) {
    message.warning(t('sys.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await systemApi.update(editingId.value, form)
      message.success(t('common.success'))
    } else {
      await systemApi.create(form)
      message.success(t('sys.created'))
    }
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function remove(row: SystemVO) {
  Modal.confirm({
    title: t('sys.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() {
      await systemApi.remove(row.id)
      message.success(t('common.success'))
      load()
    }
  })
}

const envColor = (e: string) => (e === 'PROD' ? 'red' : e === 'TEST' ? 'orange' : 'blue')
onMounted(async () => {
  domains.value = await domainApi.list()
  await load()
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('sys.title') }}</h1>
    <p class="eeip-page-desc">{{ t('sys.desc') }}</p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('sys.searchPlaceholder')" style="width: 220px"
               allow-clear @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'asset:system:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('sys.create') }}
      </a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 1100 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <span style="font-family: monospace; font-size: 12px">{{ record.code }}</span>
        </template>
        <template v-else-if="column.key === 'domainCode'">
          <a-tag color="geekblue">{{ record.domainCode || '—' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'env'">
          <a-tag :color="envColor(record.env)">{{ record.env }}</a-tag>
        </template>
        <template v-else-if="column.key === 'counts'">
          <span style="font-size: 11.5px; opacity: .8">
            {{ t('sys.connectors') }} {{ record.connectorCount }} · {{ t('sys.apis') }} {{ record.apiCount }}
          </span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'default'"
                   :text="record.status === 1 ? t('common.enabled') : t('common.disabled')" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'asset:system:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a v-hasPerm="'asset:system:delete'" style="color: #e5544b" @click="remove(record)">
              {{ t('common.delete') }}
            </a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalOpen" :title="editingId ? t('sys.editTitle') : t('sys.createTitle')"
             :confirm-loading="submitting" width="600px" :ok-text="t('common.save')"
             :cancel-text="t('common.cancel')" @ok="submit">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('sys.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="erp-core" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('sys.colName')" required>
              <a-input v-model:value="form.name" placeholder="ERP 核心系统" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('sys.colDomain')">
              <a-select v-model:value="form.domainCode">
                <a-select-option v-for="d in domains" :key="d.id" :value="d.code">
                  {{ d.name }}（{{ d.code }}）
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('sys.colEnv')">
              <a-select v-model:value="form.env">
                <a-select-option value="DEV">DEV</a-select-option>
                <a-select-option value="TEST">TEST</a-select-option>
                <a-select-option value="PROD">PROD</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('sys.colProtocol')">
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
            <a-form-item :label="t('common.status')">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">{{ t('common.enabled') }}</a-radio>
                <a-radio :value="0">{{ t('common.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('sys.baseUrl')">
              <a-input v-model:value="form.baseUrl" placeholder="http://erp.internal/api" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('sys.colOwner')"><a-input v-model:value="form.owner" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('sys.ownerPhone')"><a-input v-model:value="form.ownerPhone" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.remark')"><a-input v-model:value="form.remark" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>
