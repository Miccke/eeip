<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { userApi, type UserPayload } from '@/api/user'
import { roleApi } from '@/api/role'
import type { RoleVO, UserVO } from '@/types'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const store = useUserStore()
const loading = ref(false)
const rows = ref<UserVO[]>([])
const total = ref(0)
const roles = ref<RoleVO[]>([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const modalOpen = ref(false)
const modalTitle = ref('新增用户')
const submitting = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<UserPayload>({
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  deptName: '',
  status: 1,
  roleIds: []
})

const columns = computed(() => [
  { title: t('user.colUsername'), dataIndex: 'username', key: 'username', width: 140 },
  { title: t('user.colNickname'), dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: t('user.colDept'), dataIndex: 'deptName', key: 'deptName', width: 140 },
  { title: t('user.colRoles'), key: 'roles', width: 220 },
  { title: t('common.status'), key: 'status', width: 90 },
  { title: t('user.colLastLogin'), dataIndex: 'lastLoginAt', key: 'lastLoginAt', width: 170 },
  { title: t('common.actions'), key: 'action', width: 190, fixed: 'right' as const }
])

const roleOptions = computed(() =>
  roles.value.map((r) => ({ label: `${r.name}（${r.code}）`, value: r.id }))
)

async function load() {
  loading.value = true
  try {
    const res = await userApi.page({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined
    })
    rows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  if (!store.hasPerm('system:role:list')) return
  roles.value = await roleApi.list()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    username: '', nickname: '', password: '', email: '', phone: '',
    deptName: '', status: 1, roleIds: []
  })
}

function openCreate() {
  resetForm()
  modalTitle.value = t('user.createTitle')
  modalOpen.value = true
}

function openEdit(row: UserVO) {
  resetForm()
  editingId.value = row.id
  modalTitle.value = t('user.editTitle', { name: row.username })
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname,
    email: row.email ?? '',
    phone: row.phone ?? '',
    deptName: row.deptName ?? '',
    status: row.status,
    roleIds: [...row.roleIds]
  })
  modalOpen.value = true
}

async function submit() {
  if (!form.nickname || (!editingId.value && !form.password)) {
    message.warning(t('user.fillRequired'))
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await userApi.update(editingId.value, { ...form, password: form.password || undefined })
      message.success(t('user.saved'))
    } else {
      await userApi.create(form)
      message.success(t('user.created'))
    }
    modalOpen.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function remove(row: UserVO) {
  Modal.confirm({
    title: t('user.deleteConfirm', { name: row.nickname }),
    content: t('user.deleteHint'),
    okType: 'danger',
    async onOk() {
      await userApi.remove(row.id)
      message.success(t('user.deleted'))
      load()
    }
  })
}

const pwdOpen = ref(false)
const pwdTarget = ref<UserVO | null>(null)
const pwdValue = ref('')
const pwdSubmitting = ref(false)

function resetPwd(row: UserVO) {
  pwdTarget.value = row
  pwdValue.value = ''
  pwdOpen.value = true
}

async function submitPwd() {
  if (!pwdTarget.value) return
  if (!pwdValue.value || pwdValue.value.length < 8) {
    message.warning(t('user.pwdTooShort'))
    return
  }
  pwdSubmitting.value = true
  try {
    await userApi.resetPassword(pwdTarget.value.id, pwdValue.value)
    message.success(t('user.pwdReset'))
    pwdOpen.value = false
  } finally {
    pwdSubmitting.value = false
  }
}

function onTableChange(pagination: any) {
  query.pageNum = pagination.current
  query.pageSize = pagination.pageSize
  load()
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<template>
  <div>
    <h1 class="eeip-page-title">{{ t('user.title') }}</h1>
    <p class="eeip-page-desc">
      {{ t('user.desc') }}
    </p>

    <a-space style="margin-bottom: 12px">
      <a-input
        v-model:value="query.keyword"
        :placeholder="t('user.searchPlaceholder')"
        style="width: 240px"
        allow-clear
        @press-enter="((query.pageNum = 1), load())"
      >
        <template #prefix><SearchOutlined style="opacity: 0.4" /></template>
      </a-input>
      <a-button @click="((query.pageNum = 1), load())">{{ t('common.query') }}</a-button>
      <a-button @click="load"><ReloadOutlined /> {{ t('common.refresh') }}</a-button>
      <a-button v-hasPerm="'system:user:create'" type="primary" @click="openCreate">
        <PlusOutlined /> {{ t('user.create') }}
      </a-button>
    </a-space>

    <a-table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      size="middle"
      :scroll="{ x: 1000 }"
      :pagination="{
        current: query.pageNum,
        pageSize: query.pageSize,
        total,
        showSizeChanger: true,
        showTotal: (total: number) => t('common.total', { n: total })
      }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'roles'">
          <a-tag v-for="r in record.roleNames" :key="r" color="orange">{{ r }}</a-tag>
          <span v-if="!record.roleNames?.length" style="opacity: 0.35">{{ t('user.unassigned') }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge
            :status="record.status === 1 ? 'success' : 'default'"
            :text="record.status === 1 ? t('common.enabled') : t('common.disabled')"
          />
        </template>
        <template v-else-if="column.key === 'lastLoginAt'">
          <span style="font-family: monospace; font-size: 11.5px; opacity: 0.7">
            {{ record.lastLoginAt ? record.lastLoginAt.replace('T', ' ').slice(0, 19) : '—' }}
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a v-hasPerm="'system:user:update'" @click="openEdit(record)">{{ t('common.edit') }}</a>
            <a v-hasPerm="'system:user:reset'" @click="resetPwd(record)">{{ t('user.resetPwd') }}</a>
            <a
              v-hasPerm="'system:user:delete'"
              style="color: #e5544b"
              @click="remove(record)"
            >{{ t('common.delete') }}</a>
          </a-space>
        </template>
      </template>
    </a-table>

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
            <a-form-item :label="t('user.colUsername')" required>
              <a-input v-model:value="form.username" :disabled="!!editingId" :placeholder="t('user.colUsername')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('user.colNickname')" required>
              <a-input v-model:value="form.nickname" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="editingId ? t('user.passwordTip') : t('user.password')" :required="!editingId">
              <a-input-password v-model:value="form.password" :placeholder="t('user.passwordPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('user.colDept')">
              <a-input v-model:value="form.deptName" placeholder="如 IT/集成组" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('user.colDept')">
              <a-input v-model:value="form.email" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号">
              <a-input v-model:value="form.phone" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('user.colRoles')">
              <a-select
                v-model:value="form.roleIds"
                mode="multiple"
                :options="roleOptions"
                placeholder="选择角色"
                :disabled="!store.hasPerm('system:role:list')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="t('common.status')">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">{{ t('common.enabled') }}</a-radio>
                <a-radio :value="0">{{ t('common.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="pwdOpen"
      :title="t('user.resetTitle', { name: pwdTarget?.nickname ?? '' })"
      :confirm-loading="pwdSubmitting"
      width="420px"
      @ok="submitPwd"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item :label="t('user.newPassword')" required>
          <a-input-password v-model:value="pwdValue" :placeholder="t('user.passwordPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
