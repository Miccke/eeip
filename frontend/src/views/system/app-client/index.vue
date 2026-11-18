<script setup lang="ts">
/**
 * 应用授权：A 系统注册 → 发 appKey/appSecret → 授权可调用的接口。
 * 调用流程：签名换 JWT → 携带 JWT + MD5 签名调用接口。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { KeyOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { appClientApi, type AppClientPayload, type AppClientVO } from '@/api/appClient'
import { openApiApi, type OpenApiVO } from '@/api/openApi'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<AppClientVO[]>([])
const apis = ref<OpenApiVO[]>([])
const keyword = ref('')

const columns = computed(() => [
  { title: t('appClient.colAppKey'), dataIndex: 'appKey', key: 'appKey', width: 200 },
  { title: t('appClient.colName'), dataIndex: 'appName', key: 'appName', width: 180 },
  { title: t('appClient.colOwner'), dataIndex: 'owner', key: 'owner', width: 110 },
  { title: t('appClient.colGranted'), key: 'granted', width: 120 },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('common.actions'), key: 'action', width: 260, fixed: 'right' as const }
])

async function load() {
  loading.value = true
  try {
    rows.value = await appClientApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

/* ── 新建 / 编辑 ─────────────────────────────────── */
const modalOpen = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<AppClientPayload>({ appKey: '', appName: '', owner: '', remark: '', status: 1 })

function openCreate() {
  editingId.value = null
  Object.assign(form, { appKey: '', appName: '', owner: '', remark: '', status: 1 })
  modalOpen.value = true
}
function openEdit(row: AppClientVO) {
  editingId.value = row.id
  Object.assign(form, {
    appKey: row.appKey, appName: row.appName, owner: row.owner ?? '',
    remark: row.remark ?? '', status: row.status
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.appName) {
    message.warning(t('appClient.nameRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await appClientApi.update(editingId.value, form)
      message.success(t('common.success'))
      modalOpen.value = false
      load()
    } else {
      const created = await appClientApi.create(form)
      modalOpen.value = false
      secretTarget.value = created
      secretModal.value = true
      load()
    }
  } finally {
    submitting.value = false
  }
}

/* ── 密钥展示（仅一次）─────────────────────────── */
const secretModal = ref(false)
const secretTarget = ref<{ appKey: string; appSecret: string } | null>(null)

async function resetSecret(row: AppClientVO) {
  Modal.confirm({
    title: t('appClient.resetConfirm', { name: row.appName }),
    async onOk() {
      const s = await appClientApi.resetSecret(row.id)
      secretTarget.value = { appKey: row.appKey, appSecret: s }
      secretModal.value = true
    }
  })
}

/* ── 接口授权 ─────────────────────────────────── */
const grantOpen = ref(false)
const grantTarget = ref<AppClientVO | null>(null)
const grantIds = ref<string[]>([])

async function openGrant(row: AppClientVO) {
  grantTarget.value = row
  grantIds.value = await appClientApi.grants(row.id)
  grantOpen.value = true
}

async function saveGrant() {
  if (!grantTarget.value) return
  await appClientApi.saveGrants(grantTarget.value.id, grantIds.value)
  message.success(t('common.success'))
  grantOpen.value = false
  load()
}

function remove(row: AppClientVO) {
  Modal.confirm({
    title: t('appClient.deleteConfirm', { name: row.appName }),
    okType: 'danger',
    async onOk() {
      await appClientApi.remove(row.id)
      message.success(t('common.success'))
      load()
    }
  })
}

onMounted(async () => {
  await Promise.all([load(), openApiApi.list().then((l) => (apis.value = l))])
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('appClient.title') }}</h1>
    <p class="eeip-page-desc">{{ t('appClient.desc') }}</p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('appClient.searchPlaceholder')" style="width: 220px"
               allow-clear @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'system:app:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('appClient.create') }}
      </a-button>
      <a-button @click="message.info(t('appClient.flowHint'))">{{ t('appClient.flowHintBtn') }}</a-button>
    </a-space>

    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="middle"
             :pagination="false" :scroll="{ x: 1000 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'appKey'">
          <span style="font-family: monospace; font-size: 12px">{{ record.appKey }}</span>
        </template>
        <template v-else-if="column.key === 'granted'">
          <a-tag :color="record.grantedCount ? 'green' : 'default'">
            {{ record.grantedCount }} {{ t('appClient.apis') }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'default'"
                   :text="record.status === 1 ? t('common.enabled') : t('common.disabled')" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'system:app:grant'" @click="openGrant(record)">{{ t('appClient.grant') }}</a>
            <a v-hasPerm="'system:app:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a v-hasPerm="'system:app:update'" @click="resetSecret(record)">{{ t('appClient.resetSecret') }}</a>
            <a v-hasPerm="'system:app:delete'" style="color: #e5544b" @click="remove(record)">
              {{ t('common.delete') }}
            </a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑 -->
    <a-modal v-model:open="modalOpen" :title="editingId ? t('appClient.editTitle') : t('appClient.createTitle')"
             :confirm-loading="submitting" width="520px" :ok-text="t('common.save')"
             :cancel-text="t('common.cancel')" @ok="submit">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-form-item :label="t('appClient.colName')" required>
          <a-input v-model:value="form.appName" placeholder="A 系统" />
        </a-form-item>
        <a-form-item :label="t('appClient.colAppKey')">
          <a-input v-model:value="form.appKey" :disabled="!!editingId" placeholder="留空自动生成" />
        </a-form-item>
        <a-form-item :label="t('appClient.colOwner')">
          <a-input v-model:value="form.owner" />
        </a-form-item>
        <a-form-item :label="t('common.remark')">
          <a-input v-model:value="form.remark" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 密钥（仅一次） -->
    <a-modal v-model:open="secretModal" :title="t('appClient.secretTitle')" :footer="null" width="560px">
      <a-alert type="warning" show-icon :message="t('appClient.secretWarn')" style="margin-bottom: 12px" />
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item :label="t('appClient.colAppKey')">
          <code>{{ secretTarget?.appKey }}</code>
        </a-descriptions-item>
        <a-descriptions-item :label="t('appClient.colAppSecret')">
          <code style="word-break: break-all">{{ secretTarget?.appSecret }}</code>
        </a-descriptions-item>
      </a-descriptions>
      <div style="margin-top: 12px; text-align: right">
        <a-button type="primary" @click="secretModal = false">{{ t('appClient.savedIt') }}</a-button>
      </div>
    </a-modal>

    <!-- 接口授权 -->
    <a-modal v-model:open="grantOpen" :title="t('appClient.grantTitle', { name: grantTarget?.appName })"
             width="620px" :ok-text="t('common.save')" :cancel-text="t('common.cancel')" @ok="saveGrant">
      <a-alert type="info" show-icon :message="t('appClient.grantHint')" style="margin: 8px 0 12px" />
      <a-checkbox-group v-model:value="grantIds" style="display: block">
        <div v-for="a in apis" :key="a.id" style="padding: 6px 0; border-bottom: 1px solid rgba(24,34,44,.6)">
          <a-checkbox :value="a.id">
            <a-tag color="blue" style="margin-right: 6px">{{ a.method }}</a-tag>
            <span style="font-family: monospace; font-size: 12px">{{ a.fullPath }}</span>
            <span style="opacity: .55; margin-left: 8px">{{ a.name }}</span>
          </a-checkbox>
        </div>
      </a-checkbox-group>
      <a-empty v-if="!apis.length" :description="t('appClient.noApis')" />
    </a-modal>
  </div>
</template>
