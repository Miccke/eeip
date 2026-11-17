<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { tenantApi, type TenantPayload, type TenantVO } from '@/api/tenant'
import { post } from '@/api/request'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<TenantVO[]>([])
const keyword = ref('')

const modalOpen = ref(false)
const modalTitle = ref('新增租户')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<TenantPayload>({
  code: '',
  name: '',
  tenantType: 'PARTNER',
  status: 1,
  contactName: '',
  contactPhone: '',
  remark: ''
})

/* ── 一键开通 ───────────────────────────────────────────── */
const provOpen = ref(false)
const provLoading = ref(false)
const provForm = reactive({
  code: '',
  name: '',
  tenantType: 'PARTNER',
  adminUsername: 'admin',
  adminNickname: '',
  contactName: '',
  contactPhone: ''
})
const credOpen = ref(false)
const credResult = ref<any>(null)

function openProvision() {
  Object.assign(provForm, {
    code: '', name: '', tenantType: 'PARTNER',
    adminUsername: 'admin', adminNickname: '', contactName: '', contactPhone: ''
  })
  provOpen.value = true
}

async function submitProvision() {
  if (!provForm.code || !provForm.name) {
    message.warning(t('tenant.fillRequired'))
    return
  }
  provLoading.value = true
  try {
    const r: any = await post('/v1/system/tenants/provision', provForm)
    credResult.value = r
    provOpen.value = false
    credOpen.value = true
    load()
  } finally {
    provLoading.value = false
  }
}

function copyCred() {
  const r = credResult.value
  const text = `租户编码：${r.tenantCode}\n用户名：${r.adminUsername}\n初始密码：${r.initialPassword}`
  navigator.clipboard?.writeText(text).then(() => message.success('已复制'))
}

const columns = computed(() => [
  { title: t('tenant.colCode'), dataIndex: 'code', key: 'code', width: 160 },
  { title: t('tenant.colName'), dataIndex: 'name', key: 'name', width: 180 },
  { title: t('tenant.colType'), key: 'type', width: 110 },
  { title: t('tenant.colContact'), dataIndex: 'contactName', key: 'contactName', width: 120 },
  { title: t('tenant.colPhone'), dataIndex: 'contactPhone', key: 'contactPhone', width: 150 },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('common.remark'), dataIndex: 'remark', key: 'remark' },
  { title: t('common.actions'), key: 'action', width: 140, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await tenantApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  modalTitle.value = t('tenant.createTitle')
  Object.assign(form, {
    code: '', name: '', tenantType: 'PARTNER', status: 1,
    contactName: '', contactPhone: '', remark: ''
  })
  modalOpen.value = true
}

function openEdit(row: TenantVO) {
  editingId.value = row.id
  modalTitle.value = t('tenant.editTitle', { name: row.name })
  Object.assign(form, {
    code: row.code,
    name: row.name,
    tenantType: row.tenantType,
    status: row.status,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    remark: row.remark ?? ''
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.code || !form.name) {
    message.warning(t('tenant.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await tenantApi.update(editingId.value, form)
      message.success(t('tenant.saved'))
    } else {
      await tenantApi.create(form)
      message.success(t('tenant.created'))
    }
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function remove(row: TenantVO) {
  Modal.confirm({
    title: t('tenant.deleteConfirm', { name: row.name }),
    content: t('tenant.deleteHint'),
    okType: 'danger',
    async onOk() {
      await tenantApi.remove(row.id)
      message.success(t('tenant.deleted'))
      load()
    }
  })
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('tenant.title') }}</h1>
    <p class="eeip-page-desc">
      {{ t('tenant.desc') }}
    </p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('tenant.searchPlaceholder')" style="width: 220px" allow-clear
               @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'system:tenant:create'" type="primary" @click="openProvision">
        <PlusOutlined /> {{ t('tenant.provision') }}
      </a-button>
      <a-button v-hasPerm="'system:tenant:create'" @click="openCreate">{{ t('tenant.createOnly') }}</a-button>
    </a-space>

    <a-table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
      :scroll="{ x: 1000 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <span style="font-family: monospace; font-size: 12px">{{ record.code }}</span>
        </template>
        <template v-else-if="column.key === 'type'">
          <a-tag :color="record.tenantType === 'INTERNAL' ? 'blue' : 'orange'">
            {{ record.tenantType === 'INTERNAL' ? t('tenant.internal') : t('tenant.partner') }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'default'"
                   :text="record.status === 1 ? t('common.enabled') : t('common.disabled')" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'system:tenant:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a
              v-hasPerm="'system:tenant:delete'"
              :style="{ color: String(record.id) === '1' ? '#5b6b79' : '#e5544b' }"
              @click="String(record.id) !== '1' && remove(record)"
            >{{ t('common.delete') }}</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 一键开通 -->
    <a-modal
      v-model:open="provOpen"
      :title="t('tenant.provision')"
      :confirm-loading="provLoading"
      width="560px"
      :ok-text="t('tenant.provisionOk')"
      @ok="submitProvision"
    >
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 14px"
        :message="t('tenant.provisionHint')"
      />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('tenant.colCode')" required>
              <a-input v-model:value="provForm.code" :placeholder="t('tenant.codePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colName')" required>
              <a-input v-model:value="provForm.name" :placeholder="t('tenant.namePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.adminUser')">
              <a-input v-model:value="provForm.adminUsername" placeholder="admin" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.adminName')">
              <a-input v-model:value="provForm.adminNickname" :placeholder="t('tenant.adminNamePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colContact')">
              <a-input v-model:value="provForm.contactName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colPhone')">
              <a-input v-model:value="provForm.contactPhone" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 开通结果 -->
    <a-modal v-model:open="credOpen" :title="t('tenant.provisionDone')" :footer="null" width="480px">
      <a-alert
        type="warning"
        show-icon
        style="margin-bottom: 14px"
        :message="t('tenant.pwdWarn')"
      />
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item :label="t('tenant.colCode')">{{ credResult?.tenantCode }}</a-descriptions-item>
        <a-descriptions-item :label="t('login.username')">{{ credResult?.adminUsername }}</a-descriptions-item>
        <a-descriptions-item :label="t('user.password')">
          <code style="font-size: 13px">{{ credResult?.initialPassword }}</code>
        </a-descriptions-item>
        <a-descriptions-item :label="t('tenant.granted')">{{ credResult?.permissionCount }}</a-descriptions-item>
      </a-descriptions>
      <a-space style="margin-top: 16px">
        <a-button type="primary" @click="copyCred">{{ t('tenant.saveCred') }}</a-button>
        <a-button @click="credOpen = false">{{ t('tenant.savedIt') }}</a-button>
      </a-space>
    </a-modal>

    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="submitting"
      width="560px"
      @ok="submit"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('tenant.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="如 SUPPLIER_B" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colName')" required>
              <a-input v-model:value="form.name" placeholder="如 供应商B" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colType')">
              <a-select v-model:value="form.tenantType">
                <a-select-option value="INTERNAL">{{ t('tenant.internal') }}</a-select-option>
                <a-select-option value="PARTNER">{{ t('tenant.partner') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('common.status')">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">{{ t('common.enabled') }}</a-radio>
                <a-radio :value="0">{{ t('common.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colContact')">
              <a-input v-model:value="form.contactName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('tenant.colPhone')">
              <a-input v-model:value="form.contactPhone" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.remark')">
              <a-input v-model:value="form.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert
        v-if="!editingId"
        type="info"
        show-icon
        :message="t('tenant.createHint')"
      />
    </a-modal>
  </div>
</template>
