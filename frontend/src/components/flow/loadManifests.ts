/**
 * 加载本前端自带的节点规格。
 *
 * 规格源码放在 frontend/src/node-packages 下（前端是 UI 的主场），
 * 构建期由 scripts/sync-node-packages.mjs 同步到后端做 config 校验，
 * 因此前端无需再请求 /node-types —— 也顺带解决了"后端不可用打不开编排器"。
 */
import type { NodeManifest } from '@/api/nodeType'

const modules = import.meta.glob('../../node-packages/*/node.json', { eager: true })

export function loadManifests(): NodeManifest[] {
  return Object.values(modules)
    .map((m: any) => (m.default ?? m) as NodeManifest)
    .filter((m) => !!m?.type)
    .sort((a, b) => (a.meta?.order ?? 999) - (b.meta?.order ?? 999))
}
