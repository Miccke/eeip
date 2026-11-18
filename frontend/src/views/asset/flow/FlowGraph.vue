<script setup lang="ts">
/**
 * 只读流程图：从 START 出发按层做 BFS 布局，纯 SVG 渲染。
 * S8 会替换为可拖拽画布，这里先保证"看得懂结构、看得出环"。
 */
import { computed } from 'vue'
import type { FlowNode } from '@/api/flow'

const props = defineProps<{ nodes: FlowNode[] }>()

const NODE_W = 150
const NODE_H = 46
const GAP_X = 34
const GAP_Y = 74

/** 节点类型配色 */
const COLOR: Record<string, string> = {
  START: '#39cfbf',
  END: '#8b98a5',
  HTTP_INVOKE: '#f2a33c',
  TRANSFORM: '#6aa9ff',
  CHOICE: '#c88bff'
}

const edges = computed(() => {
  const list: { from: string; to: string; label?: string }[] = []
  for (const n of props.nodes ?? []) {
    if (n.next) list.push({ from: n.id, to: n.next })
    for (const b of n.branches ?? []) {
      if (b.next) list.push({ from: n.id, to: b.next, label: b.otherwise ? 'else' : 'when' })
    }
  }
  return list
})

/** 分层：BFS 记录每个节点的最小层级；不可达节点放到最后一层 */
const layout = computed(() => {
  const nodes = props.nodes ?? []
  const byId = new Map(nodes.map((n) => [n.id, n]))
  const level = new Map<string, number>()
  const start = nodes.find((n) => n.type === 'START') ?? nodes[0]
  if (start) {
    const queue: [string, number][] = [[start.id, 0]]
    level.set(start.id, 0)
    while (queue.length) {
      const [id, d] = queue.shift()!
      for (const e of edges.value.filter((x) => x.from === id)) {
        if (!level.has(e.to) && byId.has(e.to)) {
          level.set(e.to, d + 1)
          queue.push([e.to, d + 1])
        }
      }
    }
  }
  const maxLevel = Math.max(0, ...[...level.values()])
  for (const n of nodes) {
    if (!level.has(n.id)) level.set(n.id, maxLevel + 1)
  }

  const rows = new Map<number, string[]>()
  for (const [id, lv] of level) {
    if (!rows.has(lv)) rows.set(lv, [])
    rows.get(lv)!.push(id)
  }

  const pos = new Map<string, { x: number; y: number }>()
  for (const [lv, ids] of [...rows].sort((a, b) => a[0] - b[0])) {
    ids.forEach((id, i) => {
      const totalW = ids.length * NODE_W + (ids.length - 1) * GAP_X
      const startX = (Math.max(totalW, NODE_W) === totalW ? 0 : 0)
      pos.set(id, { x: startX + i * (NODE_W + GAP_X), y: lv * (NODE_H + GAP_Y) })
    })
  }
  const width = Math.max(1, ...[...pos.values()].map((p) => p.x + NODE_W)) + 8
  const height = Math.max(1, ...[...pos.values()].map((p) => p.y + NODE_H)) + 8
  return { pos, width, height, byId }
})

function edgePath(from: string, to: string) {
  const a = layout.value.pos.get(from)
  const b = layout.value.pos.get(to)
  if (!a || !b) return ''
  const x1 = a.x + NODE_W / 2
  const y1 = a.y + NODE_H
  const x2 = b.x + NODE_W / 2
  const y2 = b.y
  // 回边（指向更上层/同层）：从右侧绕行，让环在图上直接看得见
  if (y2 <= y1) {
    const xr = Math.max(a.x, b.x) + NODE_W + 26
    return `M${x1},${y1} C${xr},${y1 + 18} ${xr},${y2 - 18} ${x2},${y2}`
  }
  if (Math.abs(x1 - x2) < 2) return `M${x1},${y1} L${x2},${y2}`
  const mid = (y1 + y2) / 2
  return `M${x1},${y1} C${x1},${mid} ${x2},${mid} ${x2},${y2}`
}
</script>

<template>
  <div class="flow-graph">
    <svg :width="layout.width" :height="layout.height" :viewBox="`0 0 ${layout.width} ${layout.height}`">
      <defs>
        <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6" fill="#3a4a5a" />
        </marker>
      </defs>

      <g v-for="(e, i) in edges" :key="'e' + i">
        <path :d="edgePath(e.from, e.to)" stroke="#2a3744" stroke-width="1.4" fill="none"
              marker-end="url(#arrow)" />
      </g>

      <g v-for="n in nodes" :key="n.id" :transform="`translate(${layout.pos.get(n.id)?.x ?? 0},${layout.pos.get(n.id)?.y ?? 0})`">
        <rect :width="NODE_W" :height="NODE_H" rx="3" fill="#0c1218"
              :stroke="COLOR[n.type] ?? '#3a4a5a'" stroke-width="1.2" />
        <rect :width="3" :height="NODE_H" :fill="COLOR[n.type] ?? '#3a4a5a'" />
        <text x="12" y="19" fill="#dfe7ee" font-size="11.5">{{ n.name || n.id }}</text>
        <text x="12" y="35" :fill="COLOR[n.type] ?? '#8b98a5'" font-size="9.5" font-family="monospace">
          {{ n.type }} · {{ n.id }}
        </text>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.flow-graph {
  overflow: auto;
  padding: 12px;
  background: #080d12;
  border: 1px solid var(--eeip-line);
  border-radius: 2px;
  max-height: 420px;
}
</style>
