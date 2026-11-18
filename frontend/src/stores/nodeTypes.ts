import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { NodeManifest } from '@/api/nodeType'
import { adaptManifests, setActiveSpecs, resetActiveSpecs } from '@/components/flow/registry'
import { loadManifests } from '@/components/flow/loadManifests'

/**
 * 节点规格：直接读前端自带的 node-packages，无需网络。
 * 后端通过构建期同步拿到同一份文件做 config 校验，两边不会漂移。
 */
export const useNodeTypeStore = defineStore('nodeTypes', () => {
  const manifests = ref<NodeManifest[]>([])
  const ready = ref(false)

  function load(force = false) {
    if (ready.value && !force) return manifests.value
    try {
      manifests.value = loadManifests()
      setActiveSpecs(adaptManifests(manifests.value))
    } catch {
      resetActiveSpecs()
    } finally {
      ready.value = true
    }
    return manifests.value
  }

  return { manifests, ready, load }
})
