<script setup lang="ts">
/**
 * 画布：网格、节点、连线、拖拽、框选、连线交互、缩放、拾取放置模式。
 *
 * 交互约定：
 * - 空白拖拽 = 框选；Alt/中键拖拽 = 平移画布；Ctrl/⌘ + 滚轮 = 缩放
 * - 节点按下并拖动 = 移动（松开落位）
 * - 拾取模式：点一下节点"拿起"，移动到目标位置再点一下"放下"
 * - 右侧端口：点击进入连线模式再点目标节点；也可按住直接拖到目标
 * - 连线悬停出现删除按钮
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
  /** 拾取放置模式：点一下拿起、再点一下放下 */
  placement?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', id: string | null, additive: boolean): void
  (e: 'select-many', ids: string[]): void
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
const linking = reactive({ from: '', x: 0, y: 0, moved: false })
const hoverEdge = ref<number | null>(null)

/** 框选矩形（画布坐标） */
const marquee = reactive({ on: false, x0: 0, y0: 0, x1: 0, y1: 0 })
/** 拾取模式下的"手上节点" */
const carry = reactive({ id: '', moved: false })

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
  if (b.x <= a.x) return { x: (a.x + b.x) / 2, y: Math.max(a.y, b.y) + 55 }
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

/** 矩形与节点的相交判定（框选） */
function intersect(): string[] {
  const x1 = Math.min(marquee.x0, marquee.x1)
  const x2 = Math.max(marquee.x0, marquee.x1)
  const y1 = Math.min(marquee.y0, marquee.y1)
  const y2 = Math.max(marquee.y0, marquee.y1)
  if (x2 - x1 < 4 && y2 - y1 < 4) return []
  return props.nodes
    .filter((n) => {
      const s = sizeOf(n)
      const nx = n.position?.x ?? 0
      const ny = n.position?.y ?? 0
      return nx < x2 && nx + s.w > x1 && ny < y2 && ny + s.h > y1
    })
    .map((n) => n.id)
}

/* ── 节点拖拽 / 拾取 ──────────────────────────────────── */
function onNodeDown(e: PointerEvent, n: FlowNode) {
  if (e.button === 1) return
  const p = toCanvas(e)

  // 拾取模式：第一次按下拿起，之后跟随，再按下放下
  if (props.placement) {
    if (carry.id) {
      carry.id = ''
      drag.id = ''
      return
    }
    carry.id = n.id
    carry.moved = false
    drag.startX = p.x
    drag.startY = p.y
    drag.dx = p.x - (n.position?.x ?? 0)
    drag.dy = p.y - (n.position?.y ?? 0)
    emit('select', n.id, false)
    return
  }

  drag.id = n.id
  drag.moved = false
  drag.startX = p.x
  drag.startY = p.y
  drag.dx = p.x - (n.position?.x ?? 0)
  drag.dy = p.y - (n.position?.y ?? 0)
  emit('select', n.id, e.shiftKey || e.metaKey)
}

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
  const p = toCanvas(e)

  // 拾取模式下点击空白 = 放下
  if (props.placement && carry.id) {
    const n = nodeById(carry.id)
    if (n) emit('move', n.id, { x: Math.round(p.x - drag.dx), y: Math.round(p.y - drag.dy) })
    carry.id = ''
    return
  }

  if (e.button === 1 || e.altKey) {
    panning.on = true
    panning.x = e.clientX
    panning.y = e.clientY
    panning.sl = board.value!.scrollLeft
    panning.st = board.value!.scrollTop
    return
  }
  if (linking.from) {
    linking.from = ''
    emit('connect-mode', null)
    return
  }

  // 空白处按下：开始框选
  marquee.on = true
  marquee.x0 = marquee.x1 = p.x
  marquee.y0 = marquee.y1 = p.y
  if (!e.shiftKey) emit('select', null, false)
}

function onPointerMove(e: PointerEvent) {
  if (panning.on) {
    board.value!.scrollLeft = panning.sl - (e.clientX - panning.x)
    board.value!.scrollTop = panning.st - (e.clientY - panning.y)
    return
  }
  const p = toCanvas(e)

  if (marquee.on) {
    marquee.x1 = p.x
    marquee.y1 = p.y
    return
  }
  if (carry.id) {
    if (Math.abs(p.x - drag.startX) > 4 || Math.abs(p.y - drag.startY) > 4) carry.moved = true
    emit('move', carry.id, {
      x: Math.max(0, Math.round(p.x - drag.dx)),
      y: Math.max(0, Math.round(p.y - drag.dy))
    })
    return
  }
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

function onNodeUp(_e: PointerEvent, n: FlowNode) {
  if (linking.from) {
    if (linking.from !== n.id) emit('connect', linking.from, n.id)
    linking.from = ''
    emit('connect-mode', null)
  }
}

function onGlobalUp() {
  // 拾取模式下若发生了拖动，松开即落位 —— 保证"按住拖动"在任何模式下都能放下
  if (carry.id && carry.moved) {
    carry.id = ''
    carry.moved = false
  }
  if (marquee.on) {
    const ids = intersect()
    marquee.on = false
    if (ids.length) emit('select-many', ids)
  }
  drag.id = ''
  drag.moved = false
  panning.on = false
  if (linking.from && linking.moved) {
    linking.from = ''
    emit('connect-mode', null)
  }
}

function onNodeClick(n: FlowNode) {
  if (linking.from && linking.from !== n.id) {
    emit('connect', linking.from, n.id)
    linking.from = ''
    emit('connect-mode', null)
  }
}

function onWheel(e: WheelEvent) {
  if (!(e.ctrlKey || e.metaKey)) return
  e.preventDefault()
  const next = Math.min(1.6, Math.max(0.5, props.zoom + (e.deltaY > 0 ? -0.08 : 0.08)))
  emit('zoom', +next.toFixed(2))
}

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
    :class="{ 'is-linking': !!linking.from, 'is-placing': placement && carry.id }"
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
              class="fc-edge" :class="{ 'is-hover': hoverEdge === i }"
              marker-end="url(#fc-arrow)"
              @pointerenter="hoverEdge = i" @pointerleave="hoverEdge = null"
            />
            <text v-if="e.label"
                  :x="midOf(nodeById(e.from)!, nodeById(e.to)!).x"
                  :y="midOf(nodeById(e.from)!, nodeById(e.to)!).y - 6"
                  :fill="e.label === 'else' ? '#5B6B79' : '#C99A3E'"
                  font-size="9" font-family="ui-monospace, monospace">{{ e.label }}</text>
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

      <!-- 框选矩形 -->
      <div v-if="marquee.on" class="fc-marquee" :style="{
        left: Math.min(marquee.x0, marquee.x1) + 'px',
        top: Math.min(marquee.y0, marquee.y1) + 'px',
        width: Math.abs(marquee.x1 - marquee.x0) + 'px',
        height: Math.abs(marquee.y1 - marquee.y0) + 'px'
      }" />

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
          'is-warn': issueOf.get(n.id) === 'warn',
          'is-carry': carry.id === n.id
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
        <span v-if="specOf(n.type).outputs" class="fc-port fc-port--r"
              @pointerdown.stop="onPortDown($event, n)" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.fc-board { position: relative; flex: 1; min-height: 0; overflow: auto; background: #070b0f; cursor: default; }
.fc-board.is-linking { cursor: crosshair; }
.fc-board.is-placing { cursor: grabbing; }
.fc-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(to right, rgba(255,255,255,.028) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255,255,255,.028) 1px, transparent 1px);
  background-size: 20px 20px;
}
.fc-surface { position: relative; margin: 10px auto; transform-origin: 0 0; }
.fc-svg { position: absolute; inset: 0; overflow: visible; }

.fc-edge { stroke: #2f3f4e; stroke-width: 1.4; fill: none; transition: stroke .15s, stroke-width .15s; cursor: pointer; }
.fc-edge:hover, .fc-edge.is-hover { stroke: #f2a33c; stroke-width: 2.2; }
.fc-edge-del { cursor: pointer; }
.fc-edge-del circle { fill: #0b1116; stroke: #e5544b; stroke-width: 1; }

.fc-marquee {
  position: absolute; border: 1px solid rgba(242,163,60,.7);
  background: rgba(242,163,60,.08); pointer-events: none; z-index: 3;
}

.fc-node {
  position: absolute; background: #0E151D; border: 1px solid #22303c;
  padding: 8px 10px 8px 13px; cursor: grab; user-select: none;
  transition: border-color .15s, background .15s, box-shadow .15s;
}
.fc-node:active { cursor: grabbing; }
.fc-node::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--nc, #3A4A59); }
.fc-node__t { font-family: ui-monospace, monospace; font-size: 8.5px; color: #5b6b79; letter-spacing: .06em; }
.fc-node__n { font-size: 12px; color: #e6edf3; margin-top: 3px; line-height: 1.3; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fc-node__m { font-family: ui-monospace, monospace; font-size: 9px; color: #5b6b79; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fc-node:hover { border-color: #35485A; }
.fc-node.is-sel { border-color: #7a6a3f; }
.fc-node.is-main { border-color: #f2a33c; background: #121A22; }
.fc-node.is-main::after { content: ""; position: absolute; right: -4px; bottom: -4px; width: 7px; height: 7px; border: 1px solid #f2a33c; }
.fc-node.is-start { background: #0E1A15; border-color: #2C4A3A; }
.fc-node.is-end { background: #1A1214; border-color: #4A2C30; }
.fc-node.is-error { border-color: #e5544b !important; box-shadow: 0 0 0 1px rgba(229,84,75,.3); }
.fc-node.is-warn { border-color: #c99a3e !important; }
.fc-node.is-carry { box-shadow: 0 8px 24px rgba(0,0,0,.6); cursor: grabbing; }

.fc-port { position: absolute; width: 9px; height: 9px; background: #0A0F14; border: 1px solid #46617A; border-radius: 50%; transition: background .15s, border-color .15s, transform .15s; }
.fc-port--l { left: -5px; top: 50%; margin-top: -4.5px; }
.fc-port--r { right: -5px; top: 50%; margin-top: -4.5px; cursor: crosshair; }
.fc-port--r:hover { background: #f2a33c; border-color: #f2a33c; transform: scale(1.25); }
.is-linking .fc-port { border-color: #f2a33c; }
</style>
