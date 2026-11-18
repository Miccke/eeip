import { get } from './request'

/** 节点规格（后端下发的 manifest，节选前端用到的部分） */
export interface NodeManifest {
  kind: string
  apiVersion: string
  type: string
  version: string
  meta: {
    category: 'trigger' | 'transform' | 'route' | 'invoke' | 'control'
    displayName: Record<string, string>
    description?: Record<string, string>
    color?: string
    size?: { w: number; h: number }
    summary?: string
    keywords?: string[]
  }
  ports: {
    inputs: PortSpec[]
    outputs: PortSpec[]
  }
  config?: JsonSchema
  behavior?: Record<string, unknown>
  runtime?: Record<string, unknown>
  permissions?: string[]
}

export interface PortSpec {
  id: string
  label?: Record<string, string>
  type?: string
  required?: boolean
  multiple?: boolean
}

export interface JsonSchema {
  type?: string
  required?: string[]
  additionalProperties?: boolean
  properties?: Record<string, JsonSchemaProperty>
}

export interface JsonSchemaProperty {
  type?: string
  title?: string
  default?: unknown
  enum?: unknown[]
  minimum?: number
  maximum?: number
  pattern?: string
  'x-eeip'?: {
    labelKey?: string
    widget?: string
    placeholder?: string
    language?: string
    filter?: Record<string, unknown>
    dependsOn?: Record<string, unknown>
  }
}

/** 拉取节点规格清单 */
export const nodeTypeApi = {
  list: () => get<NodeManifest[]>('/v1/integration/node-types'),
  one: (type: string) => get<NodeManifest>(`/v1/integration/node-types/${type}`)
}
