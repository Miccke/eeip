<script setup lang="ts">
/** 领域管理：集团口径的业务领域，系统与接口按领域归类 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { domainApi, type Domain } from '@/api/domain'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<Domain[]>([])
const modalOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<Partial<Domain>>({ code: '', name: '', nameEn: '', sort: 999, status: 1, remark: '' })

const columns = computed(() => [
  { title: t('domain.colSort'), dataIndex: 'sort', key: 'sort', width: 80 },
  { title: t('domain.colCode'), dataIndex: 'code', key: 'code', width: 120 },
  { title: t('domain.colName'), dataIndex: 'name', key: 'name', width: 160 },
  { title: t('domain.colNameEn'), dataIndex: 'nameEn', key: 'nameEn', width: 180 },
  { title: t('common.status'), key: 'status', width: 90 },
  { title: t('common.remark'), dataIndex: 'remark', key: 'remark' },
  { title: t('common.actions'), key: 'action', width: 130, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try { rows.value = await domainApi.list() } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', nameEn: '', sort: 999, status: 1, remark: '' })
  modalOpen.value = true
}
function openEdit(row: Domain) {
  editingId.value = row.id
  Object.assign(form, { ...row })
  modalOpen.value = true
}
async function submit() {
  if (!form.code || !form.name) { message.warning(t('domain.fillRequired')); return }
  submitting.value = true
  try {
    if (editingId.value) await domainApi.update(editingId.value, form)
    else await domainApi.create(form)
    message.success(t('common.success'))
    modalOpen.value = false
    load()
  } finally { submitting.value = false }
}
function remove(row: Domain) {
  Modal.confirm({
    title: t('domain.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() { await domainApi.remove(row.id); message.success(t('common.success')); load() }
  })
}
onMounted(load)
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('domain.title') }}</h1>
    <p class="eeip-page-desc">{{ t('domain.desc') }}</p>
    <a-space style="margin-bottom: 12px">
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'asset:domain:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('domain.create') }}
      </a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 900 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <span style="font-family: monospace; font-size: 12px">{{ record.code }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'default'"
                   :text="record.status === 1 ? t('common.enabled') : t('common.disabled')" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'asset:domain:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a v-hasPerm="'asset:domain:delete'" style="color: #e5544b" @click="remove(record)">
              {{ t('common.delete') }}
            </a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalOpen" :title="editingId ? t('domain.editTitle') : t('domain.createTitle')"
             :confirm-loading="submitting" width="520px" :ok-text="t('common.save')"
             :cancel-text="t('common.cancel')" @ok="submit">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-row :gutter="16">
          <a-col :span="10">
            <a-form-item :label="t('domain.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="SCM" />
            </a-form-item>
          </a-col>
          <a-col :span="14">
            <a-form-item :label="t('domain.colName')" required>
              <a-input v-model:value="form.name" placeholder="供应链" />
            </a-form-item>
          </a-col>
          <a-col :span="14">
            <a-form-item :label="t('domain.colNameEn')"><a-input v-model:value="form.nameEn" /></a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item :label="t('domain.colSort')">
              <a-input-number v-model:value="form.sort" :min="0" :max="9999" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.remark')"><a-input v-model:value="form.remark" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>
