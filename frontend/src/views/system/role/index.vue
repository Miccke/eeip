<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { roleApi, type RolePayload } from '@/api/role'
import type { PermNodeVO, RoleVO } from '@/types'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<RoleVO[]>([])
const keyword = ref('')

const modalOpen = ref(false)
const modalTitle = ref('新增角色')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<RolePayload>({
  code: '', name: '', dataScope: 'SELF', sort: 99, status: 1, remark: ''
})

const drawerOpen = ref(false)
const drawerRole = ref<RoleVO | null>(null)
const permTree = ref<PermNodeVO[]>([])
const checkedKeys = ref<string[]>([])
const saving = ref(false)

const columns = computed(() => [
  { title: t('role.colName'), dataIndex: 'name', key: 'name', width: 160 },
  { title: t('role.colCode'), dataIndex: 'code', key: 'code', width: 170 },
  { title: t('role.colScope'), dataIndex: 'dataScope', key: 'dataScope', width: 120 },
  { title: t('role.colMembers'), dataIndex: 'userCount', key: 'userCount', width: 90 },
  { title: t('role.colSort'), dataIndex: 'sort', key: 'sort', width: 80 },
  { title: t('role.colType'), key: 'builtin', width: 100 },
  { title: t('role.colRemark'), dataIndex: 'remark', key: 'remark' },
  { title: t('common.actions'), key: 'action', width: 190, fixed: 'right' as const }
])

const scopeText = (s: string) =>
  ({ ALL: t('role.scopeAll'), TENANT: t('role.scopeTenant'), PROJECT: t('role.scopeProject'), SELF: t('role.scopeSelf') }[s] ?? s)

const treeData = computed(() =>
  permTree.value.map(function map(n: PermNodeVO): any {
    return {
      key: n.id,
      title: `${n.name}${n.code ? '  ·  ' + n.code : ''}`,
      children: n.children?.map(map)
    }
  })
)

async function load() {
  loading.value = true
  try {
    rows.value = await roleApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  modalTitle.value = t('role.createTitle')
  Object.assign(form, { code: '', name: '', dataScope: 'SELF', sort: 99, status: 1, remark: '' })
  modalOpen.value = true
}

function openEdit(row: RoleVO) {
  editingId.value = row.id
  modalTitle.value = t('role.editTitle', { name: row.name })
  Object.assign(form, {
    code: row.code, name: row.name, dataScope: row.dataScope,
    sort: row.sort, status: row.status, remark: row.remark ?? ''
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.code || !form.name) {
    message.warning(t('role.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await roleApi.update(editingId.value, form)
      message.success(t('role.saved'))
    } else {
      await roleApi.create(form)
      message.success(t('role.created'))
    }
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function remove(row: RoleVO) {
  Modal.confirm({
    title: t('role.deleteConfirm', { name: row.name }),
    okType: 'danger',
    async onOk() {
      await roleApi.remove(row.id)
      message.success(t('role.deleted'))
      load()
    }
  })
}

async function openAssign(row: RoleVO) {
  drawerRole.value = row
  drawerOpen.value = true
  permTree.value = await roleApi.permissionTree()
  checkedKeys.value = await roleApi.permissionIds(row.id)
}

async function saveAssign() {
  if (!drawerRole.value) return
  saving.value = true
  try {
    await roleApi.assign(drawerRole.value.id, checkedKeys.value)
    message.success(t('role.assignSuccess'))
    drawerOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('role.title') }}</h1>
    <p class="eeip-page-desc">
      {{ t('role.desc') }}
    </p>

    <a-space style="margin-bottom: 12px">
      <a-input v-model:value="keyword" :placeholder="t('role.searchPlaceholder')" style="width: 220px" allow-clear
               @press-enter="load" />
      <a-button @click="load">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'system:role:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('role.create') }}
      </a-button>
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
        <template v-if="column.key === 'dataScope'">
          <span style="font-family: monospace; font-size: 11.5px">{{ scopeText(record.dataScope) }}</span>
        </template>
        <template v-else-if="column.key === 'builtin'">
          <a-tag :color="record.builtin ? 'blue' : 'default'">
            {{ record.builtin ? t('role.builtin') : t('role.custom') }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'system:role:assign'" @click="openAssign(record)">{{ t('role.assign') }}</a>
            <a v-hasPerm="'system:role:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a
              v-hasPerm="'system:role:delete'"
              :style="{ color: record.builtin ? '#5b6b79' : '#e5544b' }"
              @click="!record.builtin && remove(record)"
            >{{ t('common.delete') }}</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="submitting"
      width="520px"
      @ok="submit"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('role.colCode')" required>
              <a-input v-model:value="form.code" :disabled="!!editingId" placeholder="如 ORDER_ADMIN" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('role.colName')" required>
              <a-input v-model:value="form.name" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('role.colScope')">
              <a-select v-model:value="form.dataScope">
                <a-select-option value="ALL">{{ t('role.scopeAll') }}</a-select-option>
                <a-select-option value="TENANT">{{ t('role.scopeTenant') }}</a-select-option>
                <a-select-option value="PROJECT">{{ t('role.scopeProject') }}</a-select-option>
                <a-select-option value="SELF">{{ t('role.scopeSelf') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('role.colSort')">
              <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('role.colRemark')">
              <a-input v-model:value="form.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="drawerOpen"
      :title="t('role.assignTitle', { name: drawerRole?.name ?? '' })"
      width="460"
      :body-style="{ paddingTop: '12px' }"
    >
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 12px"
        :message="t('role.assignHint')"
      />
      <a-tree
        v-model:checked-keys="checkedKeys"
        :tree-data="treeData"
        checkable
        default-expand-all
        :selectable="false"
      />
      <template #footer>
        <a-space>
          <a-button @click="drawerOpen = false">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="saveAssign">{{ t('common.save') }}</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>
