<script setup lang="ts">
/**
 * 流程编排器（1:1 对齐原型 02-flow-designer.html）
 *
 * 组件拆分：
 *   components/flow/registry.ts   节点规格（唯一扩展点）
 *   components/flow/useFlowModel  节点/连线/选中/对齐
 *   components/flow/FlowCanvas    画布渲染与交互
 * 本文件只负责组装：调色板 / 工具条 / 画布 / 属性面板 / 调试台。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  AlignCenterOutlined, ArrowLeftOutlined, ColumnWidthOutlined, DeleteOutlined,
  DragOutlined, MinusOutlined, PlusOutlined, VerticalAlignMiddleOutlined
} from '@ant-design/icons-vue'
import { flowApi, type FlowDsl, type FlowNode, type FlowValidateResult } from '@/api/flow'
import { PALETTE, buildPalette, specOf, type NodeType } from '@/components/flow/registry'
import { useNodeTypeStore } from '@/stores/nodeTypes'
import { useFlowModel } from '@/components/flow/useFlowModel'
import FlowCanvas from '@/components/flow/FlowCanvas.vue'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const flowId = ref('')
const isNew = computed(() => route.query.mode === 'new' || route.params.id === 'new')

const nodeTypeStore = useNodeTypeStore()
const model = useFlowModel()
const { nodes, selectedId, selected, activeIds, edges } = model

const meta = reactive({ code: '', name: '未命名流程', description: '', triggerType: 'HTTP' })
const version = ref('1.0.0')
const status = ref('DRAFT')
const dirty = ref(false)
const saving = ref(false)
const validating = ref(false)
const validation = ref<FlowValidateResult | null>(null)
const zoom = ref(1)
const linkingFrom = ref<string | null>(null)
/** 拾取放置模式：点一下拿起、再点一下放下（适合触控板与精细摆放） */
const placement = ref(false)
function togglePlacement() {
  placement.value = !placement.value
}

/** 校验结果按节点归集，画布上直接标红标黄 */
const issueOf = computed(() => {
  const map = new Map<string, 'error' | 'warn'>()
  for (const w of validation.value?.warnings ?? []) if (w.nodeId) map.set(w.nodeId, 'warn')
  for (const e of validation.value?.errors ?? []) if (e.nodeId) map.set(e.nodeId, 'error')
  return map
})

/* ── 调色板（后端下发，失败回落内置） ────────────────── */
const paletteGroups = computed(() =>
  buildPalette(nodeTypeStore.manifests as any, locale.value))

function onPaletteClick(entry: { type?: NodeType }) {
  if (!entry.type) {
    message.info(t('designer.soonNode'))
    return
  }
  model.addNode(entry.type)
  dirty.value = true
}

function onPaletteDragStart(e: DragEvent, entry: { type?: NodeType }) {
  if (!entry.type) {
    e.preventDefault()
    return
  }
  e.dataTransfer?.setData('application/x-eeip-node', entry.type)
  e.dataTransfer!.effectAllowed = 'copy'
}

/* ── 画布事件 ─────────────────────────────────────────── */
function onMove(id: string, pos: { x: number; y: number }) {
  const n = nodes.value.find((x) => x.id === id)
  if (n) {
    n.position = pos
    dirty.value = true
  }
}

function onConnect(from: string, to: string) {
  if (model.connect(from, to)) dirty.value = true
}

function onAddAt(type: NodeType, pos: { x: number; y: number }) {
  model.addNode(type, pos)
  dirty.value = true
}

/* ── 属性面板 ─────────────────────────────────────────── */
const activeTab = ref<'base' | 'input' | 'exception'>('base')
const cfgText = ref('')
const spec = computed(() => (selected.value ? specOf(selected.value.type) : null))

watch(selectedId, () => {
  activeTab.value = 'base'
  syncCfgText()
})

function syncCfgText() {
  const n = selected.value
  if (!n) return
  // 按规格补齐默认值，避免面板出现空字段
  const cfg = (n.config ?? {}) as Record<string, unknown>
  for (const f of specOf(n.type).fields) {
    if (cfg[f.key] == null && f.fallback != null) cfg[f.key] = f.fallback
  }
  n.config = cfg
  cfgText.value = JSON.stringify(cfg, null, 2)
}

function applyCfgText() {
  if (!selected.value) return
  try {
    selected.value.config = JSON.parse(cfgText.value || '{}')
    dirty.value = true
  } catch {
    message.error(t('flow.jsonInvalid'))
  }
}

function fieldValue(key: string): any {
  return (selected.value?.config as any)?.[key]
}
function setField(key: string, v: any) {
  if (!selected.value) return
  selected.value.config = { ...(selected.value.config ?? {}), [key]: v }
  dirty.value = true
}

const cfgField = (k: string) => (selected.value?.config as any)?.[k]
function setCfgField(k: string, v: any) {
  if (!selected.value) return
  selected.value.config = { ...(selected.value.config ?? {}), [k]: v }
  syncCfgText()
  dirty.value = true
}

/** 上游节点：谁连到了我 */
const upstreamNodes = computed(() => {
  if (!selected.value) return []
  const id = selected.value.id
  return nodes.value.filter((n) =>
    n.next === id || (n.branches ?? []).some((b) => b.next === id))
})

/** 可选输入来源：触发入参 / 上游节点输出 */
const inputOptions = computed(() => {
  const opts = [{ label: '$input（上游输出）', value: '$input' }]
  for (const u of upstreamNodes.value) {
    const v = (u.config as any)?.outputVar
    if (v) opts.push({ label: `$${v}（来自 ${u.id}）`, value: `$${v}` })
  }
  return opts
})

const nextOptions = computed(() =>
  nodes.value.filter((n) => n.id !== selectedId.value).map((n) => ({ label: `${n.id} · ${n.name}`, value: n.id })))

function addBranch() {
  if (!selected.value) return
  selected.value.branches = [...(selected.value.branches ?? []), { when: '', next: '' }]
  dirty.value = true
}
function removeBranch(i: number) {
  selected.value?.branches?.splice(i, 1)
  dirty.value = true
}
function deleteSelected() {
  if (!selected.value) return
  Modal.confirm({
    title: t('designer.deleteConfirm', { name: selected.value.name }),
    okType: 'danger',
    onOk: () => {
      model.removeNode(selectedId.value!)
      dirty.value = true
    }
  })
}

/* ── 对齐工具 ─────────────────────────────────────────── */
const alignButtons = [
  { key: 'left', icon: AlignCenterOutlined, label: 'designer.alignLeft' },
  { key: 'centerH', icon: ColumnWidthOutlined, label: 'designer.alignCenterH' },
  { key: 'right', icon: AlignCenterOutlined, label: 'designer.alignRight' },
  { key: 'top', icon: VerticalAlignMiddleOutlined, label: 'designer.alignTop' },
  { key: 'centerV', icon: VerticalAlignMiddleOutlined, label: 'designer.alignCenterV' },
  { key: 'bottom', icon: VerticalAlignMiddleOutlined, label: 'designer.alignBottom' }
] as const

function doAlign(mode: string) {
  model.align(mode as any)
  dirty.value = true
}

/* ── DSL / 校验 / 保存 / 发布 ────────────────────────── */
function buildDsl(): FlowDsl {
  return {
    schemaVersion: '1.0',
    flowCode: meta.code,
    name: meta.name,
    nodes: nodes.value.map((n) => ({
      ...n,
      next: specOf(n.type).outputs && n.type !== 'CHOICE' ? (n.next || undefined) : undefined,
      branches: n.type === 'CHOICE' ? n.branches : undefined
    }))
  }
}

const consoleTab = ref<'input' | 'output'>('input')
const consoleLines = ref<string[]>([])
function pushConsole(kind: 'input' | 'output', payload: unknown) {
  consoleTab.value = kind
  consoleLines.value = JSON.stringify(payload, null, 2).split('\n')
}

async function validate() {
  validating.value = true
  try {
    const r = await flowApi.validate(buildDsl())
    validation.value = r
    pushConsole('output', {
      valid: r.valid, nodeCount: r.nodeCount, dslHash: r.dslHash,
      errors: r.errors, warnings: r.warnings
    })
    message[r.valid ? 'success' : 'error'](r.valid ? t('designer.validatePass') : t('designer.validateFail'))
  } finally {
    validating.value = false
  }
}

/* 新建流程时补填编码与名称 */
const metaModalOpen = ref(false)
const metaDraft = reactive({ code: '', name: '' })

function save() {
  if (!meta.code || !meta.name) {
    metaDraft.code = meta.code || ''
    metaDraft.name = meta.name || ''
    metaModalOpen.value = true
    return
  }
  doSave()
}

function confirmMeta() {
  if (!/^[a-z][a-z0-9-]{2,63}$/.test(metaDraft.code)) {
    message.warning(t('flow.codeRule'))
    return
  }
  if (!metaDraft.name.trim()) {
    message.warning(t('flow.nameRule'))
    return
  }
  meta.code = metaDraft.code
  meta.name = metaDraft.name.trim()
  metaModalOpen.value = false
  doSave()
}

async function doSave() {
  saving.value = true
  try {
    const payload = {
      code: meta.code, name: meta.name, triggerType: meta.triggerType,
      description: meta.description, changeLog: '', dsl: buildDsl()
    }
    if (isNew.value || !flowId.value) {
      const id = await flowApi.create(payload)
      flowId.value = String(id)
      dirty.value = false
      message.success(t('designer.saved'))
      router.replace({ path: `/asset/flow/${id}` })
    } else {
      await flowApi.update(flowId.value, payload)
      dirty.value = false
      message.success(t('designer.saved'))
    }
    validation.value = null
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!flowId.value) {
    message.warning(t('designer.saveFirst'))
    return
  }
  try {
    const r = await flowApi.publish(flowId.value)
    status.value = 'PUBLISHED'
    version.value = r.version ?? version.value
    message.success(t('designer.published', { v: r.version }))
  } catch { /* 拦截器已提示 */ }
}

const debug = () => message.info(t('designer.debugHint'))

/* ── 初始化 ───────────────────────────────────────────── */
onMounted(async () => {
  await nodeTypeStore.load()
  if (isNew.value) {
    // 新建：画布为空，由用户从调色板添加
    meta.name = ''
    meta.code = ''
    nodes.value = []
    model.select(null)
    pushConsole('input', { schemaVersion: '1.0', nodes: [] })
    return
  }
  flowId.value = String(route.params.id ?? '')
  const d = await flowApi.detail(flowId.value)
  meta.code = d.code
  meta.name = d.name
  meta.description = d.description ?? ''
  meta.triggerType = d.triggerType
  version.value = d.version ?? '1.0.0'
  status.value = d.status
  model.setNodes((d.dsl?.nodes ?? []).map((n, i) => ({
    ...n,
    position: n.position ?? { x: 80 + (i % 3) * 260, y: 80 + Math.floor(i / 3) * 140 }
  })))
  model.resetSeq(nodes.value.length + 1)
  model.select(nodes.value[0]?.id ?? null)
  pushConsole('input', d.dsl ?? {})
})

const dragType = ref<NodeType | null>(null)

/** 同一组件实例内切换流程时重新加载，保证内容与流程绑定 */
watch(() => route.params.id, async (id) => {
  if (!id || id === 'new') return
  const d = await flowApi.detail(String(id))
  meta.code = d.code
  meta.name = d.name
  meta.description = d.description ?? ''
  meta.triggerType = d.triggerType
  version.value = d.version ?? '1.0.0'
  status.value = d.status
  model.setNodes((d.dsl?.nodes ?? []).map((n, i) => ({
    ...n,
    position: n.position ?? { x: 80 + (i % 3) * 260, y: 80 + Math.floor(i / 3) * 140 }
  })))
  model.resetSeq(nodes.value.length + 1)
  model.select(null)
  validation.value = null
  pushConsole('input', d.dsl ?? {})
})
</script>

<template>
  <div class="designer-page">
    <!-- 工具条 -->
    <div class="dz-bar">
      <a-button size="small" type="text" @click="router.push('/asset/flow')">
        <ArrowLeftOutlined />
      </a-button>
      <b class="dz-bar__name" :class="{ 'is-empty': !meta.name }">
        {{ meta.name || t('flow.untitled') }}
      </b>
      <span class="dz-badge" :class="{ 'is-live': status === 'PUBLISHED' }">
        <i />v{{ version }} {{ status === 'PUBLISHED' ? '' : t('designer.draft') }}
      </span>
      <span class="dz-dim">{{ meta.code || '—' }}</span>
      <span class="dz-dim">· {{ nodes.length }} {{ t('designer.nodes') }}</span>
      <span v-if="activeIds.length > 1" class="dz-dim">· {{ t('designer.multi', { n: activeIds.length }) }}</span>

      <!-- 对齐工具 -->
      <div class="dz-align" :title="t('designer.alignTip')">
        <a-tooltip v-for="b in alignButtons" :key="b.key" :title="t(b.label)">
          <button class="dz-tool" @click="doAlign(b.key)"><component :is="b.icon" /></button>
        </a-tooltip>
        <span class="dz-sep" />
        <a-tooltip :title="t('designer.distH')">
          <button class="dz-tool" @click="model.distribute('h'); dirty = true">⇹</button>
        </a-tooltip>
        <a-tooltip :title="t('designer.distV')">
          <button class="dz-tool" @click="model.distribute('v'); dirty = true">⇕</button>
        </a-tooltip>
        <span class="dz-sep" />
        <a-tooltip :title="t('designer.placementTip')">
          <button class="dz-tool" :class="{ 'is-on': placement }" @click="togglePlacement">
            <DragOutlined />
          </button>
        </a-tooltip>
      </div>

      <div style="margin-left:auto;display:flex;gap:8px">
        <a-button size="small" :loading="validating" v-hasPerm="'asset:flow:validate'" @click="validate">
          {{ t('designer.validate') }}
        </a-button>
        <a-button size="small" @click="debug">{{ t('designer.debug') }}</a-button>
        <a-button size="small" :loading="saving" v-hasPerm="'asset:flow:create'" @click="save">
          {{ t('designer.save') }}
        </a-button>
        <a-button size="small" type="primary" v-hasPerm="'asset:flow:publish'" @click="publish">
          {{ t('designer.publish') }}
        </a-button>
      </div>
    </div>

    <div class="designer">
      <!-- 调色板：所有条目可点击/拖拽，未实现的给出提示 -->
      <aside class="palette">
        <div v-for="g in paletteGroups" :key="g.group">
          <div class="palette__g">{{ t(g.groupKey) }}</div>
          <div
            v-for="item in g.items"
            :key="item.key"
            class="pnode"
            :class="{ 'is-plan': !item.type }"
            :draggable="!!item.type"
            @click="onPaletteClick(item)"
            @dragstart="onPaletteDragStart($event, item)"
          >
            <i :style="{ background: item.color }" />{{ item.label || t(item.labelKey) }}
            <span v-if="!item.type" class="pnode__tag">{{ t('designer.soon') }}</span>
          </div>
        </div>
      </aside>

      <!-- 画布 + 调试台 -->
      <div class="dz-center">
        <div class="board-wrap">
          <FlowCanvas
            :nodes="nodes"
            :edges="edges"
            :selected-id="selectedId"
            :active-ids="activeIds"
            :issue-of="issueOf"
            :zoom="zoom"
            @select="model.select"
            @select-many="model.selectMany"
            :placement="placement"
            @move="onMove"
            @connect="onConnect"
            @connect-mode="linkingFrom = $event"
            @remove-edge="model.removeEdge($event); dirty = true"
            @add-at="onAddAt"
            @zoom="zoom = $event"
          />
          <div class="zoom">
            <button @click="zoom = Math.max(0.5, +(zoom - 0.1).toFixed(2))">−</button>
            <button style="width:44px" @click="zoom = 1">{{ Math.round(zoom * 100) }}%</button>
            <button @click="zoom = Math.min(1.6, +(zoom + 0.1).toFixed(2))">+</button>
          </div>
          <div v-if="placement" class="link-hint link-hint--mode">
            {{ t('designer.placementOn') }}
            <a @click="placement = false">{{ t('designer.cancelLink') }}</a>
          </div>
          <div v-if="linkingFrom" class="link-hint">
            {{ t('designer.linkMode') }}
            <a @click="linkingFrom = null">{{ t('designer.cancelLink') }}</a>
          </div>
        </div>

        <!-- 调试台 -->
        <div class="console">
          <div class="console__bar">
            <span class="ctab" :class="{ 'is-on': consoleTab === 'input' }" @click="consoleTab = 'input'">
              {{ t('designer.consoleInput') }}
            </span>
            <span class="ctab" :class="{ 'is-on': consoleTab === 'output' }" @click="consoleTab = 'output'">
              {{ t('designer.consoleOutput') }}
            </span>
            <span class="ctab">{{ t('designer.consoleTiming') }}</span>
            <span class="dz-dim dz-mono" style="margin-left:auto">
              <template v-if="validation">
                {{ validation.valid ? 'valid' : 'invalid' }} · {{ validation.nodeCount }} nodes ·
                {{ validation.dslHash?.slice(0, 12) }}
              </template>
              <template v-else>trace —</template>
            </span>
          </div>
          <div class="console__body">
            <div v-if="!consoleLines.length" class="kc">{{ t('designer.consoleEmpty') }}</div>
            <div v-for="(line, i) in consoleLines" :key="i" class="cline">{{ line }}</div>
          </div>
        </div>
      </div>

      <!-- 属性面板（由节点规格驱动） -->
      <aside class="insp">
        <template v-if="selected && spec">
          <div class="insp__head">
            <b>{{ selected.name }}</b>
            <span>{{ selected.type }} · {{ selected.id }}</span>
          </div>
          <div class="tabs">
            <span class="tab" :class="{ 'is-on': activeTab === 'base' }" @click="activeTab = 'base'">
              {{ t('designer.base') }}
            </span>
            <span class="tab" :class="{ 'is-on': activeTab === 'input' }" @click="activeTab = 'input'; syncCfgText()">
              {{ t('designer.input') }}
            </span>
            <span class="tab" :class="{ 'is-on': activeTab === 'exception' }" @click="activeTab = 'exception'">
              {{ selected.type === 'CHOICE' ? t('designer.branchTab') : t('designer.exception') }}
            </span>
          </div>

          <template v-if="activeTab === 'base'">
            <div class="fld">
              <label>{{ t('designer.fName') }}</label>
              <a-input :value="selected.name" size="small"
                       @update:value="(v: string) => { selected!.name = v; dirty = true }" />
            </div>

            <!-- 规格驱动字段 -->
            <div v-for="f in spec.fields.filter(x => x.kind !== 'textarea')" :key="f.key" class="fld">
              <label>{{ f.label || t(f.labelKey) }}</label>
              <a-select v-if="f.kind === 'select'" :value="fieldValue(f.key)" size="small" style="width:100%"
                        :options="f.options" @update:value="(v: any) => setField(f.key, v)" />
              <a-input-number v-else-if="f.kind === 'number'" :value="fieldValue(f.key)" size="small"
                              :min="f.min" :max="f.max" style="width:100%"
                              @update:value="(v: any) => setField(f.key, v)" />
              <a-input v-else :value="fieldValue(f.key)" size="small" :placeholder="f.placeholder"
                       @update:value="(v: string) => setField(f.key, v)" />
            </div>

            <div v-if="selected.type !== 'END' && selected.type !== 'CHOICE'" class="fld">
              <label>{{ t('designer.fNext') }}</label>
              <a-select :value="selected.next" size="small" style="width:100%" allow-clear
                        :options="nextOptions"
                        @update:value="(v: any) => { selected!.next = v; dirty = true }" />
            </div>

            <div class="fld" style="border-bottom:0">
              <a-button danger size="small" block @click="deleteSelected">
                <DeleteOutlined /> {{ t('designer.delNode') }}
              </a-button>
            </div>
          </template>

          <template v-else-if="activeTab === 'input'">
            <div class="fld">
              <label>{{ t('designer.inputFrom') }}</label>
              <div class="insp-hint" v-if="upstreamNodes.length">
                <span v-for="u in upstreamNodes" :key="u.id" class="insp-chip">{{ u.id }} · {{ u.name }}</span>
              </div>
              <div class="insp-hint" v-else>{{ t('designer.inputNone') }}</div>
            </div>
            <div class="fld">
              <label>{{ t('designer.inputExpr') }}</label>
              <a-select :value="cfgField('inputExpr') || '$input'" size="small" style="width:100%"
                        :options="inputOptions"
                        @update:value="(v: any) => setCfgField('inputExpr', v)" />
            </div>
            <div class="fld">
              <label>{{ t('designer.fOutputVar') }}</label>
              <a-input :value="cfgField('outputVar')" size="small" placeholder="resp"
                       @update:value="(v: string) => setCfgField('outputVar', v)" />
            </div>
            <div class="fld" style="border-bottom:0">
              <label>config (JSON)</label>
              <a-textarea v-model:value="cfgText" :rows="6" style="font-family:monospace;font-size:11px"
                          @blur="applyCfgText" />
            </div>
          </template>

          <template v-else>
            <template v-if="selected.type === 'CHOICE'">
              <div v-for="(b, i) in selected.branches || []" :key="i" class="fld">
                <label>{{ t('designer.fExpression') }} #{{ i + 1 }}</label>
                <a-input :value="b.when" size="small" :disabled="b.otherwise"
                         placeholder="$resp.code == 200"
                         @update:value="(v: string) => { b.when = v; dirty = true }" />
                <div style="display:flex;gap:6px;margin-top:6px">
                  <a-select :value="b.next" size="small" style="flex:1" :options="nextOptions"
                            @update:value="(v: any) => { b.next = v; dirty = true }" />
                  <a-button size="small" danger @click="removeBranch(i)">{{ t('designer.delBranch') }}</a-button>
                </div>
              </div>
              <div class="fld" style="border-bottom:0">
                <a-button size="small" block @click="addBranch">
                  <PlusOutlined /> {{ t('designer.addBranch') }}
                </a-button>
              </div>
            </template>
            <div v-else class="fld">
              <div class="insp-hint">{{ t('designer.dragHint') }}</div>
            </div>
          </template>
        </template>
        <div v-else class="insp-empty">{{ t('designer.noSelection') }}</div>
      </aside>
    </div>

    <!-- 新建流程：填写编码与名称 -->
    <a-modal v-model:open="metaModalOpen" :title="t('flow.createTitle')" width="440px"
             :ok-text="t('common.save')" :cancel-text="t('common.cancel')" @ok="confirmMeta">
      <a-form layout="vertical" style="margin-top: 10px">
        <a-form-item :label="t('flow.colCode')" required>
          <a-input v-model:value="metaDraft.code" placeholder="demo-order-sync" />
        </a-form-item>
        <a-form-item :label="t('flow.colName')" required>
          <a-input v-model:value="metaDraft.name" placeholder="订单同步演示" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.designer-page { display: flex; flex-direction: column; height: calc(100vh - 52px); }
.dz-bar { display: flex; align-items: center; gap: 10px; height: 50px; padding: 0 16px; border-bottom: 1px solid var(--eeip-line); flex: none; }
.dz-bar__name { font-size: 13.5px; font-weight: 500; }
.dz-badge { display: inline-flex; align-items: center; gap: 5px; font-size: 10.5px; padding: 2px 7px; border: 1px solid #3a3222; color: #c99a3e; background: #1a1509; }
.dz-badge i { width: 5px; height: 5px; background: #c99a3e; border-radius: 50%; }
.dz-badge.is-live { color: #39cfbf; border-color: #1d3a36; background: #0d1a18; }
.dz-badge.is-live i { background: #39cfbf; }
.dz-dim { font-size: 10.5px; color: #5b6b79; }
.dz-mono { font-family: ui-monospace, monospace; }
.dz-align { display: flex; align-items: center; gap: 2px; margin-left: 8px; padding-left: 10px; border-left: 1px solid var(--eeip-line); }
.dz-tool { width: 24px; height: 24px; display: grid; place-items: center; background: transparent; border: 1px solid transparent; color: #7d8b98; cursor: pointer; font-size: 12px; }
.dz-tool:hover { border-color: #2a3744; color: #f2a33c; background: #0d141a; }
.dz-tool.is-on { color: #f2a33c; border-color: #3a3222; background: rgba(242,163,60,.1); }
.dz-bar__name.is-empty { color: #5b6b79; font-weight: 400; }
.dz-sep { width: 1px; height: 14px; background: var(--eeip-line); margin: 0 4px; }

.designer { flex: 1; display: grid; grid-template-columns: 180px 1fr 280px; min-height: 0; }

.palette { border-right: 1px solid var(--eeip-line); background: #0a0f14; overflow: auto; padding: 10px 0 20px; }
.palette__g { padding: 10px 12px 5px; font-size: 10.5px; color: #5b6b79; letter-spacing: .06em; }
.pnode { display: flex; align-items: center; gap: 8px; padding: 6px 12px; font-size: 12px; color: #a9b7c3; cursor: pointer; transition: background .15s, color .15s; }
.pnode:hover { background: rgba(255,255,255,.04); color: #e6edf3; }
.pnode i { width: 3px; height: 14px; flex: none; }
.pnode.is-plan { opacity: .5; }
.pnode__tag { margin-left: auto; font-size: 9px; color: #5b6b79; border: 1px solid #22303c; padding: 0 3px; }

.dz-center { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.board-wrap { position: relative; flex: 1; min-height: 0; display: flex; }
.zoom { position: absolute; right: 14px; bottom: 14px; z-index: 6; display: flex; border: 1px solid #22303c; background: rgba(8,12,17,.92); }
.zoom button { width: 26px; height: 26px; display: grid; place-items: center; color: #a9b7c3; font-family: monospace; font-size: 12px; border: 0; border-right: 1px solid var(--eeip-line); background: transparent; cursor: pointer; }
.zoom button:last-child { border: 0; }
.link-hint { position: absolute; top: 12px; left: 50%; transform: translateX(-50%); background: rgba(242,163,60,.12); border: 1px solid rgba(242,163,60,.4); color: #f2a33c; font-size: 11.5px; padding: 4px 10px; z-index: 7; }
.link-hint a { margin-left: 8px; color: #ffd9a3; }
.link-hint--mode { top: 46px; }

.console { border-top: 1px solid var(--eeip-line); background: #0a0f14; height: 168px; display: flex; flex-direction: column; flex: none; }
.console__bar { height: 32px; display: flex; align-items: center; gap: 4px; padding: 0 10px; border-bottom: 1px solid var(--eeip-line); flex: none; }
.ctab { padding: 4px 9px; font-size: 11px; color: #5b6b79; cursor: pointer; }
.ctab.is-on { color: #f2a33c; background: rgba(242,163,60,.1); }
.console__body { flex: 1; overflow: auto; padding: 9px 12px; font-family: ui-monospace, monospace; font-size: 11px; line-height: 1.7; color: #a9b7c3; }
.cline { white-space: pre-wrap; word-break: break-all; }
.kc { color: #5b6b79; font-family: ui-monospace, monospace; font-size: 11px; }

.insp { border-left: 1px solid var(--eeip-line); background: #0a0f14; overflow: auto; display: flex; flex-direction: column; }
.insp__head { padding: 12px; border-bottom: 1px solid var(--eeip-line); }
.insp__head b { font-size: 13px; font-weight: 500; }
.insp__head span { display: block; font-family: ui-monospace, monospace; font-size: 10px; color: #5b6b79; margin-top: 3px; }
.tabs { display: flex; border-bottom: 1px solid var(--eeip-line); }
.tab { padding: 8px 12px; font-size: 11.5px; color: #5b6b79; border-bottom: 1px solid transparent; margin-bottom: -1px; cursor: pointer; }
.tab.is-on { color: #e6edf3; border-bottom-color: #f2a33c; }
.fld { padding: 9px 12px; border-bottom: 1px solid rgba(24,34,44,.6); }
.fld label { display: block; font-size: 10.5px; color: #5b6b79; margin-bottom: 5px; }
.insp-hint { font-size: 11px; color: #5b6b79; line-height: 1.6; }
.insp-chip {
  display: inline-block; font-family: ui-monospace, monospace; font-size: 10.5px;
  padding: 1px 6px; margin: 0 4px 4px 0; border: 1px solid #22303c; color: #a9b7c3;
}
.insp-empty { padding: 24px 16px; font-size: 11.5px; color: #5b6b79; text-align: center; }
</style>
