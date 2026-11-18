import { defineStore } from 'pinia'
import { ref } from 'vue'
import { nodeTypeApi, type NodeManifest } from '@/api/nodeType'
import { adaptManifests, setActiveSpecs, resetActiveSpecs } from '@/components/flow/registry'

/**
 * 节点规格缓存：编排器启动时拉一次，失败回落前端内置规格。
 */
export const useNodeTypeStore = defineStore('nodeTypes', () => {
  const manifests = ref<NodeManifest[]>([])
  const ready = ref(false)
  const failed = ref(false)

  async function load(force = false) {
    if (ready.value && !force) return manifests.value
    try {
      const list = await nodeTypeApi.list()
      manifests.value = list ?? []
      setActiveSpecs(adaptManifests(manifests.value))
      failed.value = false
    } catch {
      // 后端不可用时用内置规格，保证编排器仍能打开
      resetActiveSpecs()
      failed.value = true
    } finally {
      ready.value = true
    }
    return manifests.value
  }

  return { manifests, ready, failed, load }
})
