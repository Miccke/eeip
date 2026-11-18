/**
 * 编排器模型：节点集合 + 连线 + 选中态 + 对齐操作。
 * 与渲染解耦，方便单测和后续替换画布实现。
 */
import { computed, ref } from 'vue'
import type { FlowNode } from '@/api/flow'
import { specOf, type NodeType } from './registry'

export interface Edge {
  from: string
  to: string
  label?: string
  branchIndex?: number
}

export function useFlowModel() {
  const nodes = ref<FlowNode[]>([])
  const selectedId = ref<string | null>(null)
  const multiSelected = ref<string[]>([])
  let seq = 1

  const selected = computed(() => nodes.value.find((n) => n.id === selectedId.value) ?? null)

  /** 选中集合：多选优先，否则单选 */
  const activeIds = computed(() =>
    multiSelected.value.length ? multiSelected.value : selectedId.value ? [selectedId.value] : []
  )

  const edges = computed<Edge[]>(() => {
    const list: Edge[] = []
    for (const n of nodes.value) {
      if (n.next) list.push({ from: n.id, to: n.next })
      ;(n.branches ?? []).forEach((b, i) => {
        if (b.next) {
          list.push({
            from: n.id, to: b.next, branchIndex: i,
            label: b.otherwise ? 'else' : (b.when || 'when')
          })
        }
      })
    }
    return list
  })

  /** 找一个空位放新节点：最右节点左侧空 60px，超出宽度换行 */
  function nextPosition() {
    if (!nodes.value.length) return { x: 80, y: 120 }
    const maxRight = Math.max(...nodes.value.map((n) => (n.position?.x ?? 0) + specOf(n.type).size.w))
    const anchor = nodes.value.reduce((a, b) =>
      (b.position?.x ?? 0) > (a.position?.x ?? 0) ? b : a)
    if (maxRight + 240 > 1180) {
      return { x: 80, y: Math.max(...nodes.value.map((n) => (n.position?.y ?? 0))) + 150 }
    }
    return { x: maxRight + 60, y: anchor.position?.y ?? 120 }
  }

  function addNode(type: NodeType, at?: { x: number; y: number }) {
    const spec = specOf(type)
    const id = `n${seq++}`
    const node: FlowNode = {
      id,
      type,
      name: spec.defaultName,
      config: spec.defaultConfig(),
      position: at ?? nextPosition()
    }
    if (type === 'CHOICE') node.branches = []
    else if (spec.outputs) node.next = ''
    nodes.value.push(node)
    select(id)
    return node
  }

  function select(id: string | null, additive = false) {
    if (id === null) {
      selectedId.value = null
      multiSelected.value = []
      return
    }
    if (additive) {
      const set = new Set(multiSelected.value)
      set.has(id) ? set.delete(id) : set.add(id)
      multiSelected.value = [...set]
      selectedId.value = id
      return
    }
    selectedId.value = id
    multiSelected.value = []
  }

  /** 框选：整批选中，主选中取第一个 */
  function selectMany(ids: string[]) {
    multiSelected.value = [...ids]
    selectedId.value = ids[0] ?? null
  }

  function removeNode(id: string) {
    nodes.value = nodes.value.filter((n) => n.id !== id)
    for (const n of nodes.value) {
      if (n.next === id) n.next = ''
      for (const b of n.branches ?? []) if (b.next === id) b.next = ''
    }
    if (selectedId.value === id) selectedId.value = null
    multiSelected.value = multiSelected.value.filter((x) => x !== id)
  }

  /** 连线：CHOICE 加分支，其余设置 next */
  function connect(fromId: string, toId: string) {
    if (fromId === toId) return false
    const src = nodes.value.find((n) => n.id === fromId)
    if (!src) return false
    if (src.type === 'CHOICE') {
      src.branches = src.branches ?? []
      const hasOtherwise = src.branches.some((b) => b.otherwise)
      src.branches.push(
        hasOtherwise
          ? { when: `$input.x == ${src.branches.length}`, next: toId }
          : { otherwise: true, next: toId }
      )
    } else if (specOf(src.type).outputs) {
      src.next = toId
    } else {
      return false
    }
    return true
  }

  function removeEdge(e: Edge) {
    const src = nodes.value.find((n) => n.id === e.from)
    if (!src) return
    if (e.branchIndex != null) src.branches?.splice(e.branchIndex, 1)
    else src.next = ''
  }

  /* ── 对齐与分布 ──────────────────────────────────────── */
  type Align = 'left' | 'centerH' | 'right' | 'top' | 'centerV' | 'bottom'
  const targets = () => nodes.value.filter((n) => activeIds.value.includes(n.id))

  function align(mode: Align) {
    const list = targets()
    if (list.length < 2) return
    const box = (n: FlowNode) => ({
      x: n.position?.x ?? 0, y: n.position?.y ?? 0, ...specOf(n.type).size
    })
    const minX = Math.min(...list.map((n) => box(n).x))
    const maxX = Math.max(...list.map((n) => box(n).x + box(n).w))
    const minY = Math.min(...list.map((n) => box(n).y))
    const maxY = Math.max(...list.map((n) => box(n).y + box(n).h))
    for (const n of list) {
      const b = box(n)
      const pos = { ...(n.position ?? { x: 0, y: 0 }) }
      if (mode === 'left') pos.x = minX
      if (mode === 'right') pos.x = maxX - b.w
      if (mode === 'centerH') pos.x = Math.round((minX + maxX) / 2 - b.w / 2)
      if (mode === 'top') pos.y = minY
      if (mode === 'bottom') pos.y = maxY - b.h
      if (mode === 'centerV') pos.y = Math.round((minY + maxY) / 2 - b.h / 2)
      n.position = pos
    }
  }

  function distribute(axis: 'h' | 'v') {
    const list = [...targets()].sort((a, b) =>
      axis === 'h' ? (a.position?.x ?? 0) - (b.position?.x ?? 0)
                   : (a.position?.y ?? 0) - (b.position?.y ?? 0))
    if (list.length < 3) return
    const first = list[0], last = list[list.length - 1]
    if (axis === 'h') {
      const start = (first.position?.x ?? 0) + specOf(first.type).size.w
      const end = last.position?.x ?? 0
      const gap = (end - start) / (list.length - 2)
      let cursor = start
      for (const n of list.slice(1, -1)) {
        n.position = { x: Math.round(cursor + gap / 2 - specOf(n.type).size.w / 2), y: n.position?.y ?? 0 }
        cursor += gap
      }
    } else {
      const start = (first.position?.y ?? 0) + specOf(first.type).size.h
      const end = last.position?.y ?? 0
      const gap = (end - start) / (list.length - 2)
      let cursor = start
      for (const n of list.slice(1, -1)) {
        n.position = { x: n.position?.x ?? 0, y: Math.round(cursor + gap / 2 - specOf(n.type).size.h / 2) }
        cursor += gap
      }
    }
  }

  return {
    nodes, selectedId, selected, multiSelected, activeIds, edges,
    addNode, select, selectMany, removeNode, connect, removeEdge, align, distribute,
    nextPosition,
    resetSeq: (n: number) => { seq = n },
    setNodes: (list: FlowNode[]) => { nodes.value = list }
  }
}
