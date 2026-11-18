<script setup lang="ts">
/**
 * 画布：网格、节点、连线、拖拽、框选、连线交互、缩放。
 *
 * 交互约定：
 * - 空白处拖拽 = 平移画布；滚轮 = 以光标为锚点缩放
 * - 节点上按下并移动 = 移动节点（松开即落位，不会跟随鼠标）
 * - 右侧端口：点击进入"连线模式"，再点目标节点完成；也可按住直接拖到目标
 * - 连线中点出现删除按钮
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import type { FlowNode } from '@/api/flow'
import { specOf, summaryOf, type NodeType } from './registry'
import type { Edge } from './useFlowModel'

const props = defineProps<{
  nodes: FlowNode[]
  edges: Edge[]
  selectedId: string | null
  activeIds: string[]
  issueOf: Map<string, 'error' | 'warn'>
  zoom: number
}>()

const emit = defineEmits<{
  (e: 'select', id: string | null, additive: boolean): void
  (e: 'move', id: string, pos: { x: number; y: number }): void
  (e: 'connect', from: string, to: string): void
  (e: 'connect-mode', from: string | null): void
  (e: 'remove-edge', edge: Edge): void
  (e: 'add-at', type: NodeType, pos: { x: number; y: number }): void
  (e: 'zoom', v: number): void
}>()

const board = ref<HTMLElement | null>(null)
const drag = reactive({ id: '', dx: 0, dy: 0, moved: false, startX: 0, startY: 0 })
const panning = reactive({ on: false, x: 0, y: 0, sl: 0, st: 0 })
/** 连线中：from 非空表示处于连线模式 */
const linking = reactive({ from: '', x: 0, y: 0, moved: false })
const hoverEdge = ref<number | null>(null)

const sizeOf = (n: FlowNode) => specOf(n.type).size
const colorOf = (n: FlowNode) => specOf(n.type).color

const surfaceW = computed(() =>
  Math.max(900, ...props.nodes.map((n) => (n.position?.x ?? 0) + sizeOf(n).w + 120)))
const surfaceH = computed(() =>
  Math.max(460, ...props.nodes.map((n) => (n.position?.y ?? 0) + sizeOf(n).h + 120)))

const outPort = (n: FlowNode) => ({
  x: (n.position?.x ?? 0) + sizeOf(n).w,
  y: (n.position?.y ?? 0) + sizeOf(n).h / 2
})
const inPort = (n: FlowNode) => ({
  x: n.position?.x ?? 0,
  y: (n.position?.y ?? 0) + sizeOf(n).h / 2
})

function pathOf(from: FlowNode, to: FlowNode) {
  const a = outPort(from), b = inPort(to)
  if (b.x <= a.x) {
    const dy = Math.max(a.y, b.y) + 70
    return `M${a.x},${a.y} C${a.x + 80},${dy} ${b.x - 80},${dy} ${b.x},${b.y}`
  }
  const dx = Math.max(36, (b.x - a.x) / 2)
  return `M${a.x},${a.y} C${a.x + dx},${a.y} ${b.x - dx},${b.y} ${b.x},${b.y}`
}

function midOf(from: FlowNode, to: FlowNode) {
  const a = outPort(from), b = inPort(to)
  if (b.x <= a.x) {
    return { x: (a.x + b.x) / 2, y: Math.max(a.y, b.y) + 55 }
  }
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
}

const nodeById = (id: string) => props.nodes.find((n) => n.id === id)

/* ── 坐标换算 ─────────────────────────────────────────── */
function toCanvas(e: PointerEvent | WheelEvent | MouseEvent) {
  const el = board.value!
  const r = el.getBoundingClientRect()
  return {
    x: (e.clientX - r.left + el.scrollLeft) / props.zoom,
    y: (e.clientY - r.top + el.scrollTop) / props.zoom
  }
}

/* ── 节点拖拽 ─────────────────────────────────────────── */
function onNodeDown(e: PointerEvent, n: FlowNode) {
  if (e.button === 1) return
  const p = toCanvas(e)
  drag.id = n.id
  drag.moved = false
  drag.startX = p.x
  drag.startY = p.y
  drag.dx = p.x - (n.position?.x ?? 0)
  drag.dy = p.y - (n.position?.y ?? 0)
  emit('select', n.id, e.shiftKey || e.metaKey)
}

/** 端口按下：进入连线模式 */
function onPortDown(e: PointerEvent, n: FlowNode) {
  e.stopPropagation()
  const p = toCanvas(e)
  linking.from = n.id
  linking.x = p.x
  linking.y = p.y
  linking.moved = false
  emit('connect-mode', n.id)
}

/* ── 画布交互 ─────────────────────────────────────────── */
function onBoardDown(e: PointerEvent) {
  if (e.button === 1 || e.altKey) {
    panning.on = true
    panning.x = e.clientX
    panning.y = e.clientY
    panning.sl = board.value!.scrollLeft
    panning.st = board.value!.scrollTop
    return
  }
  if (linking.from) {
    // 连线模式下点空白：取消
    linking.from = ''
    emit('connect-mode', null)
    return
  }
  emit('select', null, false)
}

function onPointerMove(e: PointerEvent) {
  if (panning.on) {
    board.value!.scrollLeft = panning.sl - (e.clientX - panning.x)
    board.value!.scrollTop = panning.st - (e.clientY - panning.y)
    return
  }
  const p = toCanvas(e)
  if (drag.id) {
    if (Math.abs(p.x - drag.startX) > 3 || Math.abs(p.y - drag.startY) > 3) drag.moved = true
    if (drag.moved) {
      emit('move', drag.id, {
        x: Math.max(0, Math.round(p.x - drag.dx)),
        y: Math.max(0, Math.round(p.y - drag.dy))
      })
    }
    return
  }
  if (linking.from) {
    linking.x = p.x
    linking.y = p.y
    linking.moved = true
  }
}

function onNodeUp(e: PointerEvent, n: FlowNode) {
  if (linking.from) {
    // 拖到目标节点上松手 → 完成连线
    if (linking.from !== n.id) {
      emit('connect', linking.from, n.id)
    }
    linking.from = ''
    emit('connect-mode', null)
    return
  }
}

function onGlobalUp() {
  drag.id = ''
  drag.moved = false
  panning.on = false
  if (linking.from && linking.moved) {
    linking.from = ''
    emit('connect-mode', null)
  }
}

/** 连线模式下点击节点即完成连线（无需拖拽） */
function onNodeClick(n: FlowNode) {
  if (linking.from && linking.from !== n.id) {
    emit('connect', linking.from, n.id)
    linking.from = ''
    emit('connect-mode', null)
  }
}

/* ── 缩放 ─────────────────────────────────────────────── */
function onWheel(e: WheelEvent) {
  if (!(e.ctrlKey || e.metaKey)) return
  e.preventDefault()
  const next = Math.min(1.6, Math.max(0.5, props.zoom + (e.deltaY > 0 ? -0.08 : 0.08)))
  emit('zoom', +next.toFixed(2))
}

/* ── 拖入调色板节点 ───────────────────────────────────── */
function onDrop(e: DragEvent) {
  const type = e.dataTransfer?.getData('application/x-eeip-node') as NodeType
  if (!type) return
  const p = toCanvas(e as unknown as MouseEvent)
  emit('add-at', type, { x: Math.round(p.x), y: Math.round(p.y) })
}

onMounted(() => {
  window.addEventListener('pointerup', onGlobalUp)
  window.addEventListener('pointermove', onPointerMove)
})
onBeforeUnmount(() => {
  window.removeEventListener('pointerup', onGlobalUp)
  window.removeEventListener('pointermove', onPointerMove)
})

defineExpose({ board })
</script>

<template>
  <div
    ref="board"
    class="fc-board"
    :class="{ 'is-linking': !!linking.from }"
    @pointerdown="onBoardDown"
    @wheel="onWheel"
    @dragover.prevent
    @drop.prevent="onDrop"
  >
    <div class="fc-grid" />
    <div class="fc-surface" :style="{ width: surfaceW + 'px', height: surfaceH + 'px', transform: `scale(${zoom})` }">
      <svg :width="surfaceW" :height="surfaceH" class="fc-svg">
        <defs>
          <marker id="fc-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
            <path d="M0,0 L6,3 L0,6" fill="#3A4A59" />
          </marker>
        </defs>

        <g v-for="(e, i) in edges" :key="i">
          <template v-if="nodeById(e.from) && nodeById(e.to)">
            <path
              :d="pathOf(nodeById(e.from)!, nodeById(e.to)!)"
              class="fc-edge"
              :class="{ 'is-hover': hoverEdge === i }"
              marker-end="url(#fc-arrow)"
              @pointerenter="hoverEdge = i"
              @pointerleave="hoverEdge = null"
            />
            <text
              v-if="e.label"
              :x="midOf(nodeById(e.from)!, nodeById(e.to)!).x"
              :y="midOf(nodeById(e.from)!, nodeById(e.to)!).y - 6"
              :fill="e.label === 'else' ? '#5B6B79' : '#C99A3E'"
              font-size="9" font-family="ui-monospace, monospace"
            >{{ e.label }}</text>
            <g v-if="hoverEdge === i" class="fc-edge-del"
               :transform="`translate(${midOf(nodeById(e.from)!, nodeById(e.to)!).x},${midOf(nodeById(e.from)!, nodeById(e.to)!).y})`"
               @click.stop="emit('remove-edge', e)">
              <circle r="8" />
              <path d="M-3,-3 L3,3 M3,-3 L-3,3" stroke="#e5544b" stroke-width="1.4" />
            </g>
          </template>
        </g>

        <path v-if="linking.from && nodeById(linking.from)"
              :d="`M${outPort(nodeById(linking.from)!).x},${outPort(nodeById(linking.from)!).y} L${linking.x},${linking.y}`"
              stroke="#f2a33c" stroke-width="1.4" stroke-dasharray="4 3" fill="none" />
      </svg>

      <div
        v-for="n in nodes"
        :key="n.id"
        class="fc-node"
        :class="{
          'is-sel': activeIds.includes(n.id),
          'is-main': selectedId === n.id,
          'is-start': n.type === 'START',
          'is-end': n.type === 'END',
          'is-error': issueOf.get(n.id) === 'error',
          'is-warn': issueOf.get(n.id) === 'warn'
        }"
        :style="{
          left: (n.position?.x ?? 0) + 'px',
          top: (n.position?.y ?? 0) + 'px',
          width: sizeOf(n).w + 'px',
          height: sizeOf(n).h + 'px',
          '--nc': colorOf(n)
        }"
        @pointerdown.stop="onNodeDown($event, n)"
        @pointerup.stop="onNodeUp($event, n)"
        @click.stop="onNodeClick(n)"
      >
        <div class="fc-node__t">{{ n.type }}</div>
        <div class="fc-node__n">{{ n.name }}</div>
        <div class="fc-node__m">{{ summaryOf(n) }}</div>

        <span v-if="specOf(n.type).inputs" class="fc-port fc-port--l" />
        <span
          v-if="specOf(n.type).outputs"
          class="fc-port fc-port--r"
          @pointerdown.stop="onPortDown($event, n)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.fc-board {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #070b0f;
  cursor: default;
}
.fc-board.is-linking { cursor: crosshair; }
.fc-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(to right, rgba(255,255,255,.028) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255,255,255,.028) 1px, transparent 1px);
  background-size: 20px 20px;
}
.fc-surface { position: relative; margin: 10px auto; transform-origin: 0 0; }
.fc-svg { position: absolute; inset: 0; overflow: visible; }

.fc-edge {
  stroke: #2f3f4e; stroke-width: 1.4; fill: none;
  transition: stroke .15s, stroke-width .15s;
  cursor: pointer;
}
.fc-edge:hover, .fc-edge.is-hover { stroke: #f2a33c; stroke-width: 2.2; }
.fc-edge-del { cursor: pointer; }
.fc-edge-del circle { fill: #0b1116; stroke: #e5544b; stroke-width: 1; }

.fc-node {
  position: absolute; background: #0E151D; border: 1px solid #22303c;
  padding: 8px 10px 8px 13px; cursor: grab; user-select: none;
  transition: border-color .15s, background .15s, box-shadow .15s;
}
.fc-node:active { cursor: grabbing; }
.fc-node::before {
  content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 3px;
  background: var(--nc, #3A4A59);
}
.fc-node__t { font-family: ui-monospace, monospace; font-size: 8.5px; color: #5b6b79; letter-spacing: .06em; }
.fc-node__n { font-size: 12px; color: #e6edf3; margin-top: 3px; line-height: 1.3; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fc-node__m { font-family: ui-monospace, monospace; font-size: 9px; color: #5b6b79; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fc-node:hover { border-color: #35485A; }
.fc-node.is-sel { border-color: #7a6a3f; }
.fc-node.is-main { border-color: #f2a33c; background: #121A22; }
.fc-node.is-main::after {
  content: ""; position: absolute; right: -4px; bottom: -4px; width: 7px; height: 7px;
  border: 1px solid #f2a33c;
}
.fc-node.is-start { background: #0E1A15; border-color: #2C4A3A; }
.fc-node.is-end { background: #1A1214; border-color: #4A2C30; }
.fc-node.is-error { border-color: #e5544b !important; box-shadow: 0 0 0 1px rgba(229,84,75,.3); }
.fc-node.is-warn { border-color: #c99a3e !important; }

.fc-port {
  position: absolute; width: 9px; height: 9px; background: #0A0F14; border: 1px solid #46617A;
  border-radius: 50%; transition: background .15s, border-color .15s, transform .15s;
}
.fc-port--l { left: -5px; top: 50%; margin-top: -4.5px; }
.fc-port--r { right: -5px; top: 50%; margin-top: -4.5px; cursor: crosshair; }
.fc-port--r:hover { background: #f2a33c; border-color: #f2a33c; transform: scale(1.25); }
.is-linking .fc-port { border-color: #f2a33c; }
</style>
