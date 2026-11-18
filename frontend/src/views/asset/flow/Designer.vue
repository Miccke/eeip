<script setup lang="ts">
/**
 * 流程编排器 —— 1:1 对齐原型 02-flow-designer.html
 * 布局：调色板(180) | 画布 + 调试台 | 属性面板(280)
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { flowApi, type FlowDsl, type FlowNode, type FlowValidateResult } from '@/api/flow'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const flowId = ref<string>('')
const isNew = computed(() => route.query.mode === 'new' || route.params.id === 'new')

/* ── 画布状态 ─────────────────────────────────────────── */
const nodes = ref<FlowNode[]>([])
const selectedId = ref<string | null>(null)
const zoom = ref(1)
const dirty = ref(false)
const saving = ref(false)
const validating = ref(false)
const validation = ref<FlowValidateResult | null>(null)
const version = ref('1.0.0')
const status = ref('DRAFT')

const meta = reactive({ code: '', name: '未命名流程', description: '', triggerType: 'HTTP' })

const SIZES: Record<string, { w: number; h: number }> = {
  START: { w: 132, h: 46 },
  END: { w: 122, h: 44 },
  HTTP_INVOKE: { w: 176, h: 64 },
  TRANSFORM: { w: 168, h: 62 },
  CHOICE: { w: 148, h: 62 }
}
const COLORS: Record<string, string> = {
  START: '#3D9E6B', END: '#B05A63', HTTP_INVOKE: '#8A6FD4',
  TRANSFORM: '#3B82C4', CHOICE: '#C99A3E'
}

function sizeOf(n: FlowNode) {
  return SIZES[n.type] ?? { w: 160, h: 60 }
}

/* ── 调色板 ───────────────────────────────────────────── */
interface PaletteItem { key: string; label: string; color: string; type?: string; disabled?: boolean }
const palette = computed<{ group: string; items: PaletteItem[] }[]>(() => [
  {
    group: t('designer.groupTrigger'),
    items: [
      { key: 'start-http', label: t('designer.tHttpStart'), color: '#3D9E6B', type: 'START' },
      { key: 'start-mq', label: t('designer.tMqStart'), color: '#3D9E6B', disabled: true },
      { key: 'start-sch', label: t('designer.tSchedule'), color: '#3D9E6B', disabled: true }
    ]
  },
  {
    group: t('designer.groupTransform'),
    items: [
      { key: 'map', label: t('designer.tMapping'), color: '#3B82C4', type: 'TRANSFORM' },
      { key: 'script', label: t('designer.tScript'), color: '#3B82C4', disabled: true }
    ]
  },
  {
    group: t('designer.groupRoute'),
    items: [
      { key: 'choice', label: t('designer.tChoice'), color: '#C99A3E', type: 'CHOICE' },
      { key: 'filter', label: t('designer.tFilter'), color: '#C99A3E', disabled: true },
      { key: 'parallel', label: t('designer.tParallel'), color: '#C99A3E', disabled: true }
    ]
  },
  {
    group: t('designer.groupInvoke'),
    items: [
      { key: 'http', label: t('designer.tHttpInvoke'), color: '#8A6FD4', type: 'HTTP_INVOKE' },
      { key: 'soap', label: t('designer.tSoap'), color: '#8A6FD4', disabled: true },
      { key: 'db', label: t('designer.tDb'), color: '#8A6FD4', disabled: true },
      { key: 'mqpub', label: t('designer.tPublish'), color: '#8A6FD4', disabled: true }
    ]
  },
  {
    group: t('designer.groupControl'),
    items: [
      { key: 'tx', label: t('designer.tTransaction'), color: '#5C6B78', disabled: true },
      { key: 'sub', label: t('designer.tSubflow'), color: '#5C6B78', disabled: true },
      { key: 'delay', label: t('designer.tDelay'), color: '#5C6B78', disabled: true },
      { key: 'end', label: t('designer.tEnd'), color: '#B05A63', type: 'END' }
    ]
  }
])

let seq = 1

/** 新节点放在最右侧节点右边；超过画布宽度则换行，避免堆叠 */
function nextPosition() {
  if (!nodes.value.length) return { x: 80, y: 100 }
  const maxX = Math.max(...nodes.value.map((n) => (n.position?.x ?? 0) + sizeOf(n).w))
  const anchor = nodes.value.reduce((a, b) =>
    (b.position?.x ?? 0) > (a.position?.x ?? 0) ? b : a)
  if (maxX + 240 > 1160) {
    return { x: 80, y: Math.max(...nodes.value.map((n) => (n.position?.y ?? 0))) + 140 }
  }
  return { x: maxX + 60, y: anchor.position?.y ?? 100 }
}

function addNode(item: PaletteItem) {
  if (!item.type) return
  const id = `n${seq++}`
  const n: FlowNode = {
    id,
    type: item.type,
    name: item.type === 'START' ? '开始' : item.type === 'END' ? '结束' :
          item.type === 'HTTP_INVOKE' ? 'HTTP 调用' :
          item.type === 'TRANSFORM' ? '数据映射' : '条件分支',
    config: item.type === 'TRANSFORM' ? { mapping: {} }
      : item.type === 'HTTP_INVOKE' ? { connectionCode: 'mock_orders', operation: 'getOrder', timeoutMs: 5000 }
      : {},
    position: nextPosition()
  }
  if (item.type === 'CHOICE') n.branches = []
  if (item.type !== 'END' && item.type !== 'CHOICE') n.next = ''
  nodes.value.push(n)
  selectedId.value = id
  dirty.value = true
}

/* ── 选中节点与连线 ───────────────────────────────────── */
const selected = computed(() => nodes.value.find((n) => n.id === selectedId.value) ?? null)

/** 校验结果按节点归集，画布上直接标出问题节点 */
const issueOf = computed(() => {
  const map = new Map<string, 'error' | 'warn'>()
  for (const w of validation.value?.warnings ?? []) {
    if (w.nodeId) map.set(w.nodeId, 'warn')
  }
  for (const e of validation.value?.errors ?? []) {
    if (e.nodeId) map.set(e.nodeId, 'error')
  }
  return map
})

const edges = computed(() => {
  const list: { from: string; to: string; label?: string; branch?: number }[] = []
  for (const n of nodes.value) {
    if (n.next) list.push({ from: n.id, to: n.next })
    ;(n.branches ?? []).forEach((b, i) => {
      if (b.next) list.push({ from: n.id, to: b.next, label: b.otherwise ? 'else' : (b.when || 'when'), branch: i })
    })
  }
  return list
})

function portOut(n: FlowNode) {
  const s = sizeOf(n)
  return { x: (n.position?.x ?? 0) + s.w, y: (n.position?.y ?? 0) + s.h / 2 }
}
function portIn(n: FlowNode) {
  const s = sizeOf(n)
  return { x: n.position?.x ?? 0, y: (n.position?.y ?? 0) + s.h / 2 }
}

function edgePath(from: FlowNode, to: FlowNode) {
  const a = portOut(from)
  const b = portIn(to)
  if (b.x <= a.x) {
    // 回边：从下方绕行
    const ym = Math.max(a.y, b.y) + 90
    return `M${a.x},${a.y} C${a.x + 70},${ym} ${b.x - 70},${ym} ${b.x},${b.y}`
  }
  const dx = Math.max(40, (b.x - a.x) / 2)
  return `M${a.x},${a.y} C${a.x + dx},${a.y} ${b.x - dx},${b.y} ${b.x},${b.y}`
}

function edgeMid(from: FlowNode, to: FlowNode) {
  const a = portOut(from)
  const b = portIn(to)
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 - 6 }
}

/* ── 拖拽节点 ─────────────────────────────────────────── */
const drag = reactive({ id: '', dx: 0, dy: 0, moved: false })

function onNodePointerDown(e: PointerEvent, n: FlowNode) {
  if (selectedId.value !== n.id) {
    selectedId.value = n.id
    syncCfgText()
  }
  drag.id = n.id
  drag.moved = false
  const rect = boardRef.value!.getBoundingClientRect()
  drag.dx = (e.clientX - rect.left) / zoom.value + boardRef.value!.scrollLeft - (n.position?.x ?? 0)
  drag.dy = (e.clientY - rect.top) / zoom.value + boardRef.value!.scrollTop - (n.position?.y ?? 0)
  try {
    ;(e.target as HTMLElement).setPointerCapture?.(e.pointerId)
  } catch { /* 合成事件没有真实指针，忽略 */ }
}

function onBoardPointerMove(e: PointerEvent) {
  if (!drag.id) return
  const rect = boardRef.value!.getBoundingClientRect()
  const n = nodes.value.find((x) => x.id === drag.id)
  if (!n) return
  const nx = Math.max(0, (e.clientX - rect.left) / zoom.value + boardRef.value!.scrollLeft - drag.dx)
  const ny = Math.max(0, (e.clientY - rect.top) / zoom.value + boardRef.value!.scrollTop - drag.dy)
  n.position = { x: Math.round(nx), y: Math.round(ny) }
  drag.moved = true
  dirty.value = true
}

function onBoardPointerUp() {
  drag.id = ''
}

/** 画布上的移动：拖节点 / 拉连线二选一 */
function onPointerMove(e: PointerEvent) {
  if (drag.id) onBoardPointerMove(e)
  else if (linking.from) onPortMove(e)
}

/* ── 端口连线 ─────────────────────────────────────────── */
const linking = reactive({ from: '', x: 0, y: 0 })
const boardRef = ref<HTMLElement | null>(null)

function onPortDown(e: PointerEvent, n: FlowNode) {
  e.stopPropagation()
  linking.from = n.id
  const rect = boardRef.value!.getBoundingClientRect()
  linking.x = (e.clientX - rect.left) / zoom.value + boardRef.value!.scrollLeft
  linking.y = (e.clientY - rect.top) / zoom.value + boardRef.value!.scrollTop
}

function onPortMove(e: PointerEvent) {
  if (!linking.from) return
  const rect = boardRef.value!.getBoundingClientRect()
  linking.x = (e.clientX - rect.left) / zoom.value + boardRef.value!.scrollLeft
  linking.y = (e.clientY - rect.top) / zoom.value + boardRef.value!.scrollTop
}

function onNodePointerUp(n: FlowNode) {
  if (!linking.from || linking.from === n.id) {
    linking.from = ''
    return
  }
  const src = nodes.value.find((x) => x.id === linking.from)
  if (src) {
    if (src.type === 'CHOICE') {
      // 连到 CHOICE：新增一个兜底分支
      src.branches = src.branches ?? []
      const hasOtherwise = src.branches.some((b) => b.otherwise)
      src.branches.push(
        hasOtherwise
          ? { when: `$x == ${src.branches.length}`, next: n.id }
          : { otherwise: true, next: n.id }
      )
    } else if (src.type !== 'END') {
      src.next = n.id
    }
    dirty.value = true
  }
  linking.from = ''
}

/* ── 属性面板 ─────────────────────────────────────────── */
const activeTab = ref<'base' | 'input' | 'exception'>('base')
const cfgText = ref('')

function syncCfgText() {
  const cfg = selected.value?.config as Record<string, unknown> | undefined
  if (cfg && selected.value?.type === 'HTTP_INVOKE' && cfg.timeoutMs == null) {
    cfg.timeoutMs = 5000
  }
  cfgText.value = JSON.stringify(selected.value?.config ?? {}, null, 2)
}
function applyCfgText() {
  if (!selected.value) return
  try {
    selected.value.config = JSON.parse(cfgText.value || '{}')
    dirty.value = true
  } catch {
    message.error('JSON 格式错误')
  }
}

function removeNode() {
  if (!selected.value) return
  const id = selected.value.id
  nodes.value = nodes.value.filter((n) => n.id !== id)
  for (const n of nodes.value) {
    if (n.next === id) n.next = ''
    for (const b of n.branches ?? []) if (b.next === id) b.next = ''
  }
  selectedId.value = null
  dirty.value = true
}

function addBranch() {
  if (!selected.value) return
  selected.value.branches = selected.value.branches ?? []
  selected.value.branches.push({ when: '', next: '' })
  dirty.value = true
}

function removeBranch(i: number) {
  selected.value?.branches?.splice(i, 1)
  dirty.value = true
}

/* ── DSL 组装 / 校验 / 保存 / 发布 ────────────────────── */
function buildDsl(): FlowDsl {
  return {
    schemaVersion: '1.0',
    flowCode: meta.code,
    name: meta.name,
    nodes: nodes.value.map((n) => ({
      ...n,
      // CHOICE 不写 next；END 不写 next
      next: n.type === 'END' || n.type === 'CHOICE' ? undefined : (n.next || undefined),
      branches: n.type === 'CHOICE' ? n.branches : undefined
    }))
  }
}

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
    if (r.valid) message.success(t('designer.validatePass'))
    else message.error(t('designer.validateFail'))
  } finally {
    validating.value = false
  }
}

async function save() {
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
    message.warning('请先保存')
    return
  }
  try {
    const r = await flowApi.publish(flowId.value)
    status.value = 'PUBLISHED'
    version.value = r.version ?? version.value
    message.success(t('designer.published', { v: r.version }))
  } catch { /* 拦截器已提示 */ }
}

function debug() {
  message.info(t('designer.debugHint'))
}

/* ── 调试台 ───────────────────────────────────────────── */
const consoleTab = ref<'input' | 'output'>('input')
const consoleLines = ref<string[]>([])

/* ── 加载 ─────────────────────────────────────────────── */
onMounted(async () => {
  if (isNew.value) {
    meta.name = '新建流程'
    addNode(palette.value[0].items[0])
    const end = palette.value[4].items.find((i) => i.key === 'end')!
    addNode(end)
    nodes.value[0].next = nodes.value[1].id
    nodes.value[0].position = { x: 60, y: 200 }
    nodes.value[1].position = { x: 420, y: 200 }
    selectedId.value = nodes.value[0].id
    syncCfgText()
    pushConsole('input', buildDsl())
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
  nodes.value = (d.dsl?.nodes ?? []).map((n, i) => ({
    ...n,
    position: n.position ?? { x: 60 + (i % 3) * 240, y: 60 + Math.floor(i / 3) * 120 }
  }))
  seq = nodes.value.length + 1
  // 默认选中第一个节点，属性面板不至于空着
  selectedId.value = nodes.value[0]?.id ?? null
  if (selectedId.value) syncCfgText()
  pushConsole('input', d.dsl ?? {})
})

const surfaceW = computed(() => {
  const maxX = Math.max(600, ...nodes.value.map((n) => (n.position?.x ?? 0) + sizeOf(n).w + 80))
  return maxX
})
const surfaceH = computed(() => {
  const maxY = Math.max(420, ...nodes.value.map((n) => (n.position?.y ?? 0) + sizeOf(n).h + 80))
  return maxY
})
</script>

<template>
  <div class="designer-page">
    <!-- 工具条 -->
    <div class="dz-bar">
      <a-button size="small" type="text" @click="router.push('/asset/flow')">
        <ArrowLeftOutlined />
      </a-button>
      <b class="dz-bar__name">{{ meta.name }}</b>
      <span class="dz-badge" :class="status === 'PUBLISHED' ? 'is-live' : ''">
        <i />v{{ version }} {{ status === 'PUBLISHED' ? '' : t('designer.draft') }}
      </span>
      <span class="dz-dim">{{ meta.code || '—' }}</span>
      <span class="dz-dim">· {{ nodes.length }} {{ t('designer.nodes') }}</span>
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
      <!-- 调色板 -->
      <aside class="palette">
        <div v-for="g in palette" :key="g.group">
          <div class="palette__g">{{ g.group }}</div>
          <div
            v-for="item in g.items"
            :key="item.key"
            class="pnode"
            :class="{ 'is-off': item.disabled }"
            :title="item.disabled ? t('designer.soon') : ''"
            @click="!item.disabled && addNode(item)"
          >
            <i :style="{ background: item.color }" />{{ item.label }}
          </div>
        </div>
      </aside>

      <!-- 画布 + 调试台 -->
      <div class="dz-center">
        <div class="board-wrap">
        <div
          ref="boardRef"
          class="board"
          @pointermove="onPointerMove($event)"
          @pointerup="onBoardPointerUp()"
        >
          <div class="board__grid" />
          <div class="surface" :style="{ width: surfaceW + 'px', height: surfaceH + 'px', transform: `scale(${zoom})`, transformOrigin: '0 0' }">
            <svg :width="surfaceW" :height="surfaceH" fill="none" style="position:absolute;inset:0;overflow:visible">
              <g v-for="(e, i) in edges" :key="'e' + i">
                <path
                  :d="edgePath(nodes.find(n => n.id === e.from)!, nodes.find(n => n.id === e.to)!)"
                  stroke="#3A4A59" stroke-width="1.4" fill="none" marker-end="url(#arrow2)"
                />
                <text
                  v-if="e.label"
                  :x="edgeMid(nodes.find(n => n.id === e.from)!, nodes.find(n => n.id === e.to)!).x"
                  :y="edgeMid(nodes.find(n => n.id === e.from)!, nodes.find(n => n.id === e.to)!).y"
                  :fill="e.label === 'else' ? '#5B6B79' : '#C99A3E'"
                  font-family="monospace" font-size="9"
                >{{ e.label }}</text>
              </g>
              <path v-if="linking.from" :d="`M${portOut(nodes.find(n=>n.id===linking.from)!).x},${portOut(nodes.find(n=>n.id===linking.from)!).y} L${linking.x},${linking.y}`"
                    stroke="#f2a33c" stroke-width="1.2" stroke-dasharray="3 3" />
              <defs>
                <marker id="arrow2" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
                  <path d="M0,0 L6,3 L0,6" fill="#3A4A59" />
                </marker>
              </defs>
            </svg>

            <div
              v-for="n in nodes"
              :key="n.id"
              class="node"
              :class="{
                'is-sel': n.id === selectedId,
                'node--start': n.type === 'START',
                'node--end': n.type === 'END',
                'node--error': issueOf.get(n.id) === 'error',
                'node--warn': issueOf.get(n.id) === 'warn'
              }"
              :style="{
                left: (n.position?.x ?? 0) + 'px',
                top: (n.position?.y ?? 0) + 'px',
                width: sizeOf(n).w + 'px',
                height: sizeOf(n).h + 'px',
                '--nc': COLORS[n.type]
              }"
              @pointerdown.stop="onNodePointerDown($event, n)"
              @pointerup.stop="onNodePointerUp(n)"
            >
              <div class="node__t">{{ n.type }}</div>
              <div class="node__n">{{ n.name }}</div>
              <div v-if="n.type === 'CHOICE'" class="node__m">{{ (n.branches || []).length }} 分支</div>
              <div v-else-if="n.type === 'HTTP_INVOKE'" class="node__m">
                {{ (n.config as any)?.connectionCode || '未配置连接' }} · {{ (n.config as any)?.timeoutMs || 5000 }}ms
              </div>
              <div v-else-if="n.type === 'TRANSFORM'" class="node__m">
                {{ Object.keys((n.config as any)?.mapping || {}).length }} 个字段
              </div>
              <span v-if="n.type !== 'START'" class="node__port node__port--l" />
              <span v-if="n.type !== 'END'" class="node__port node__port--r"
                    @pointerdown.stop="onPortDown($event, n)" />
            </div>

          </div>
          <div class="zoom">
            <button @click="zoom = Math.max(0.5, +(zoom - 0.1).toFixed(2))">−</button>
            <button style="width:44px" @click="zoom = 1">{{ Math.round(zoom * 100) }}%</button>
            <button @click="zoom = Math.min(1.6, +(zoom + 0.1).toFixed(2))">+</button>
            <button @click="zoom = 1; boardRef?.scrollTo(0, 0)">⤢</button>
          </div>
        </div>
        </div>

        <!-- 调试台 -->
        <div class="console">
          <div class="console__bar">
            <span class="ctab" :class="{ 'is-on': consoleTab === 'input' }"
                  @click="consoleTab = 'input'">{{ t('designer.consoleInput') }}</span>
            <span class="ctab" :class="{ 'is-on': consoleTab === 'output' }"
                  @click="consoleTab = 'output'">{{ t('designer.consoleOutput') }}</span>
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

      <!-- 属性面板 -->
      <aside class="insp">
        <template v-if="selected">
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
              <a-input v-model:value="selected.name" size="small" @change="dirty = true" />
            </div>

            <template v-if="selected.type === 'HTTP_INVOKE'">
              <div class="fld">
                <label>{{ t('designer.fConnection') }}</label>
                <a-input v-model:value="(selected.config as any).connectionCode" size="small"
                         placeholder="mock_orders" @change="dirty = true" />
              </div>
              <div class="fld">
                <label>{{ t('designer.fOperation') }}</label>
                <a-input v-model:value="(selected.config as any).operation" size="small"
                         placeholder="getOrder" @change="dirty = true" />
              </div>
              <div class="fld">
                <label>{{ t('designer.fOutputVar') }}</label>
                <a-input v-model:value="(selected.config as any).outputVar" size="small"
                         placeholder="resp" @change="dirty = true" />
              </div>
              <div class="fld">
                <label>{{ t('designer.fTimeout') }}</label>
                <a-input-number v-model:value="(selected.config as any).timeoutMs" size="small"
                                :min="500" :max="60000" style="width:100%" @change="dirty = true" />
              </div>
            </template>

            <template v-if="selected.type === 'TRANSFORM'">
              <div class="fld">
                <label>{{ t('designer.fOutputVar') }}</label>
                <a-input v-model:value="(selected.config as any).outputVar" size="small" @change="dirty = true" />
              </div>
            </template>

            <div class="fld" v-if="selected.type !== 'END' && selected.type !== 'CHOICE'">
              <label>{{ t('designer.fNext') }}</label>
              <a-select v-model:value="selected.next" size="small" style="width:100%" allow-clear
                        @change="dirty = true">
                <a-select-option v-for="n in nodes.filter(x => x.id !== selected!.id)" :key="n.id" :value="n.id">
                  {{ n.id }} · {{ n.name }}
                </a-select-option>
              </a-select>
            </div>

            <div class="fld" style="border-bottom:0">
              <a-button danger size="small" block @click="removeNode">{{ t('designer.delNode') }}</a-button>
            </div>
          </template>

          <template v-else-if="activeTab === 'input'">
            <div class="fld" style="border-bottom:0">
              <label>config (JSON)</label>
              <a-textarea v-model:value="cfgText" :rows="12" style="font-family:monospace;font-size:11px"
                          @blur="applyCfgText" />
            </div>
          </template>

          <template v-else>
            <template v-if="selected.type === 'CHOICE'">
              <div class="fld" v-for="(b, i) in selected.branches || []" :key="i">
                <label>{{ t('designer.fExpression') }} #{{ i + 1 }}</label>
                <a-input v-model:value="b.when" size="small" :disabled="b.otherwise"
                         placeholder="$resp.code == 200" @change="dirty = true" />
                <div style="display:flex;gap:6px;margin-top:6px">
                  <a-select v-model:value="b.next" size="small" style="flex:1" @change="dirty = true">
                    <a-select-option v-for="n in nodes.filter(x => x.id !== selected!.id)" :key="n.id" :value="n.id">
                      {{ n.id }} · {{ n.name }}
                    </a-select-option>
                  </a-select>
                  <a-button size="small" danger @click="removeBranch(i)">
                    {{ t('designer.delBranch') }}
                  </a-button>
                </div>
              </div>
              <div class="fld" style="border-bottom:0">
                <a-button size="small" block @click="addBranch">{{ t('designer.addBranch') }}</a-button>
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
  </div>
</template>

<style scoped>
.designer-page { display: flex; flex-direction: column; height: calc(100vh - 52px); }
.dz-bar {
  display: flex; align-items: center; gap: 10px;
  height: 50px; padding: 0 16px; border-bottom: 1px solid var(--eeip-line); flex: none;
}
.dz-bar__name { font-size: 13.5px; font-weight: 500; }
.dz-badge {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 10.5px; padding: 2px 7px; border: 1px solid #3a3222; color: #c99a3e; background: #1a1509;
}
.dz-badge i { width: 5px; height: 5px; background: #c99a3e; border-radius: 50%; }
.dz-badge.is-live { color: #39cfbf; border-color: #1d3a36; background: #0d1a18; }
.dz-badge.is-live i { background: #39cfbf; }
.dz-dim { font-size: 10.5px; color: #5b6b79; }
.dz-mono { font-family: ui-monospace, monospace; }

.designer { flex: 1; display: grid; grid-template-columns: 180px 1fr 280px; min-height: 0; }

/* palette */
.palette { border-right: 1px solid var(--eeip-line); background: #0a0f14; overflow: auto; padding: 10px 0 20px; }
.palette__g { padding: 10px 12px 5px; font-size: 10.5px; color: #5b6b79; letter-spacing: .06em; }
.pnode {
  display: flex; align-items: center; gap: 8px; padding: 6px 12px;
  font-size: 12px; color: #a9b7c3; cursor: pointer; transition: background .15s, color .15s;
}
.pnode:hover { background: rgba(255,255,255,.03); color: #e6edf3; }
.pnode.is-off { opacity: .35; cursor: not-allowed; }
.pnode i { width: 3px; height: 14px; flex: none; }

.dz-center { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.board-wrap { position: relative; flex: 1; min-height: 0; display: flex; }
.board { position: relative; overflow: auto; background: #070b0f; flex: 1; }
.board__grid {
  position: absolute; inset: 0;
  background-image: linear-gradient(to right, rgba(255,255,255,.028) 1px, transparent 1px),
                    linear-gradient(to bottom, rgba(255,255,255,.028) 1px, transparent 1px);
  background-size: 20px 20px;
}
.surface { position: relative; margin: 10px auto; }

.node {
  position: absolute; background: #0E151D; border: 1px solid #22303c;
  padding: 8px 10px 8px 13px; cursor: pointer; user-select: none;
  transition: border-color .15s, background .15s;
}
.node::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--nc, #3A4A59); }
.node__t { font-family: ui-monospace, monospace; font-size: 8.5px; color: #5b6b79; letter-spacing: .06em; }
.node__n { font-size: 12px; color: #e6edf3; margin-top: 3px; line-height: 1.3; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.node__m { font-family: ui-monospace, monospace; font-size: 9px; color: #5b6b79; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.node:hover { border-color: #35485A; }
.node.is-sel { border-color: #f2a33c; background: #121A22; }
.node.is-sel::after { content: ""; position: absolute; right: -4px; bottom: -4px; width: 7px; height: 7px; border: 1px solid #f2a33c; }
.node--start { background: #0E1A15; border-color: #2C4A3A; }
.node--end { background: #1A1214; border-color: #4A2C30; }
.node__port { position: absolute; width: 7px; height: 7px; background: #0A0F14; border: 1px solid #46617A; }
.node__port--l { left: -4px; top: 50%; margin-top: -3.5px; }
.node__port--r { right: -4px; top: 50%; margin-top: -3.5px; cursor: crosshair; }
.node--error { border-color: #e5544b !important; box-shadow: 0 0 0 1px rgba(229,84,75,.35); }
.node--warn { border-color: #c99a3e !important; }

.zoom { position: absolute; right: 14px; bottom: 14px; z-index: 6; display: flex; border: 1px solid #22303c; background: rgba(8,12,17,.9); }
.zoom button { width: 26px; height: 26px; display: grid; place-items: center; color: #a9b7c3; font-family: monospace; font-size: 12px; border: 0; border-right: 1px solid var(--eeip-line); background: transparent; cursor: pointer; }
.zoom button:last-child { border: 0; }

/* console */
.console { border-top: 1px solid var(--eeip-line); background: #0a0f14; height: 168px; display: flex; flex-direction: column; flex: none; }
.console__bar { height: 32px; display: flex; align-items: center; gap: 4px; padding: 0 10px; border-bottom: 1px solid var(--eeip-line); flex: none; }
.ctab { padding: 4px 9px; font-size: 11px; color: #5b6b79; cursor: pointer; }
.ctab.is-on { color: #f2a33c; background: rgba(242,163,60,.1); }
.console__body { flex: 1; overflow: auto; padding: 9px 12px; font-family: ui-monospace, monospace; font-size: 11px; line-height: 1.7; color: #a9b7c3; }
.cline { white-space: pre-wrap; word-break: break-all; }
.kc { color: #5b6b79; font-family: ui-monospace, monospace; font-size: 11px; }

/* inspector */
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
.insp-empty { padding: 24px 16px; font-size: 11.5px; color: #5b6b79; text-align: center; }
</style>
