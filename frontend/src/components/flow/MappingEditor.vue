<script setup lang="ts">
/**
 * 字段映射编辑器。
 *
 * 左侧是样本报文里的可用字段，中间是「目标字段 ← 表达式」列表，右侧是实时预览。
 * 求值由后端完成（与运行时的 TransformExecutor 同一段代码），保证预览与实际执行一致。
 */
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { flowApi } from '@/api/flow'

const props = defineProps<{
  modelValue: Record<string, any>
  sampleInput?: Record<string, any>
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: Record<string, any>): void }>()

const { t } = useI18n()

/** 行式映射（保留顺序） */
interface Row { target: string; expr: string }
const rows = ref<Row[]>([])
const sampleText = ref('')
const preview = ref<{ output?: Record<string, any>; fields?: any[]; error?: string } | null>(null)
const previewing = ref(false)
const mode = ref<'visual' | 'json'>('visual')
const jsonText = ref('')

function fromObject(obj: Record<string, any>): Row[] {
  return Object.entries(obj ?? {}).map(([target, expr]) => ({
    target,
    expr: typeof expr === 'string' ? expr : JSON.stringify(expr)
  }))
}
function toObject(list: Row[]): Record<string, any> {
  const out: Record<string, any> = {}
  for (const r of list) {
    if (!r.target) continue
    const e = r.expr?.trim() ?? ''
    // 以 $ 开头视为表达式；JSON 字面量按原样解析；其余当常量字符串
    if (e.startsWith('$')) out[r.target] = e
    else if ((e.startsWith('{') || e.startsWith('[') || e === 'true' || e === 'false' || /^-?\d+(\.\d+)?$/.test(e))) {
      try { out[r.target] = JSON.parse(e) } catch { out[r.target] = e }
    } else out[r.target] = e
  }
  return out
}

watch(
  () => props.modelValue,
  (v) => {
    rows.value = fromObject(v)
    jsonText.value = JSON.stringify(v ?? {}, null, 2)
  },
  { immediate: true, deep: true }
)

watch(
  () => props.sampleInput,
  (v) => {
    if (v && Object.keys(v).length && !sampleText.value) {
      sampleText.value = JSON.stringify(v, null, 2)
    }
  },
  { immediate: true }
)

/** 样本里可用的字段：递归展开到 3 层，数组取第 0 个元素的路径 */
const sampleFields = computed(() => {
  try {
    const obj = sampleText.value ? JSON.parse(sampleText.value) : {}
    const out: string[] = []
    const walk = (node: any, prefix: string, depth: number) => {
      if (depth > 3 || node == null || typeof node !== 'object') return
      for (const [k, v] of Object.entries(node)) {
        const path = `${prefix}.${k}`
        out.push(path)
        if (Array.isArray(v) && v.length && typeof v[0] === 'object') {
          walk(v[0], `${path}.0`, depth + 1)
        } else if (v && typeof v === 'object') {
          walk(v, path, depth + 1)
        }
      }
    }
    walk(obj, '$', 1)
    return out
  } catch {
    return []
  }
})

function commit() {
  emit('update:modelValue', toObject(rows.value))
}

function addRow() {
  rows.value.push({ target: '', expr: '' })
}
function removeRow(i: number) {
  rows.value.splice(i, 1)
  commit()
}
function pickField(i: number, f: string) {
  rows.value[i].expr = f
  commit()
}
function autoFill() {
  // 用样本字段名做目标字段，表达式指向同名源字段
  const obj = sampleText.value ? safeParse(sampleText.value) : {}
  rows.value = Object.keys(obj).map((k) => ({ target: k, expr: `$.${k}` }))
  commit()
  message.success(t('map.autoFilled', { n: rows.value.length }))
}

function safeParse(s: string) {
  try { return JSON.parse(s) } catch { return {} }
}

function applyJson() {
  const obj = safeParse(jsonText.value)
  rows.value = fromObject(obj)
  commit()
  mode.value = 'visual'
}

async function doPreview() {
  previewing.value = true
  try {
    const input = sampleText.value ? safeParse(sampleText.value) : {}
    preview.value = await flowApi.previewMapping(toObject(rows.value), input)
  } finally {
    previewing.value = false
  }
}

const outputText = computed(() =>
  preview.value?.output ? JSON.stringify(preview.value.output, null, 2) : '')
</script>

<template>
  <div class="map">
    <!-- 顶部工具条 -->
    <div class="map__bar">
      <a-radio-group v-model:value="mode" size="small" button-style="solid">
        <a-radio-button value="visual">{{ t('map.visual') }}</a-radio-button>
        <a-radio-button value="json">JSON</a-radio-button>
      </a-radio-group>
      <a-button size="small" @click="addRow"><PlusOutlined /> {{ t('map.addRow') }}</a-button>
      <a-button size="small" @click="autoFill">{{ t('map.autoFill') }}</a-button>
      <a-button size="small" type="primary" :loading="previewing" @click="doPreview">
        <ThunderboltOutlined /> {{ t('map.preview') }}
      </a-button>
      <span class="map__count">{{ t('map.rows', { n: rows.length }) }}</span>
    </div>

    <div class="map__grid">
      <!-- 左：样本 + 可用字段 -->
      <div class="pane">
        <div class="pane__head">{{ t('map.sample') }}</div>
        <textarea v-model="sampleText" class="sample" spellcheck="false"
                  :placeholder="t('map.sampleHint')" />
        <div class="pane__head">{{ t('map.fields') }}</div>
        <div class="fields">
          <span v-for="f in sampleFields" :key="f" class="field-chip">{{ f }}</span>
          <span v-if="!sampleFields.length" class="dim">{{ t('map.noFields') }}</span>
        </div>
      </div>

      <!-- 中：映射行 -->
      <div class="pane pane--mid">
        <template v-if="mode === 'visual'">
          <div class="pane__head">{{ t('map.target') }} ← {{ t('map.expr') }}</div>
          <div class="rows">
            <div v-for="(r, i) in rows" :key="i" class="row">
              <a-input v-model:value="r.target" size="small" :placeholder="t('map.targetPh')"
                       class="row__target" @blur="commit" />
              <span class="row__arrow">←</span>
              <a-input v-model:value="r.expr" size="small" :placeholder="t('map.exprPh')"
                       class="row__expr" @blur="commit" />
              <a-dropdown :trigger="['click']">
                <a class="row__pick">▾</a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item v-for="f in sampleFields" :key="f" @click="pickField(i, f)">
                      {{ f }}
                    </a-menu-item>
                    <a-menu-item v-if="!sampleFields.length" disabled>{{ t('map.noFields') }}</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a class="row__del" @click="removeRow(i)"><DeleteOutlined /></a>
            </div>
            <a-empty v-if="!rows.length" :image="null" :description="t('map.empty')" />
          </div>
        </template>
        <template v-else>
          <div class="pane__head">mapping JSON</div>
          <textarea v-model="jsonText" class="json" spellcheck="false" />
          <a-button size="small" style="margin-top: 6px" @click="applyJson">{{ t('map.applyJson') }}</a-button>
        </template>
      </div>

      <!-- 右：预览 -->
      <div class="pane">
        <div class="pane__head">{{ t('map.result') }}</div>
        <div v-if="preview?.error" class="err">{{ preview.error }}</div>
        <table v-if="preview?.fields?.length" class="ftable">
          <tr v-for="f in preview.fields" :key="f.target">
            <td class="mono">{{ f.target }}</td>
            <td class="mono dim">{{ f.expr }}</td>
            <td class="mono" :class="f.resolved ? 'ok' : 'bad'">
              {{ f.resolved ? (f.value ?? '—') : t('map.miss') }}
            </td>
          </tr>
        </table>
        <pre v-if="outputText" class="out">{{ outputText }}</pre>
        <div v-else class="dim tiny" style="padding: 8px">{{ t('map.previewHint') }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map { display: flex; flex-direction: column; gap: 8px; }
.map__bar { display: flex; align-items: center; gap: 8px; }
.map__count { margin-left: auto; font-size: 11px; color: #5b6b79; }
.map__grid { display: grid; grid-template-columns: 240px 1fr 300px; gap: 10px; }
.pane { background: #0a0f14; border: 1px solid #18222c; padding: 8px; min-height: 320px; }
.pane--mid { display: flex; flex-direction: column; }
.pane__head { font-size: 11px; color: #5b6b79; margin: 2px 0 6px; }
.sample, .json {
  width: 100%; height: 130px; background: #070b0f; border: 1px solid #141d26; color: #9fb0be;
  font-family: ui-monospace, monospace; font-size: 10.5px; padding: 6px; resize: vertical;
}
.json { height: 240px; }
.fields { display: flex; flex-wrap: wrap; gap: 4px; }
.field-chip {
  font-family: ui-monospace, monospace; font-size: 10px; color: #39cfbf;
  border: 1px solid #1d3b3a; padding: 1px 5px;
}
.rows { display: flex; flex-direction: column; gap: 5px; overflow: auto; max-height: 340px; }
.row { display: flex; align-items: center; gap: 6px; }
.row__target { width: 34%; }
.row__expr { flex: 1; }
.row__arrow { color: #5b6b79; font-size: 11px; }
.row__pick { color: #f2a33c; font-size: 11px; }
.row__del { color: #e5544b; font-size: 11px; }
.ftable { width: 100%; border-collapse: collapse; font-size: 10.5px; }
.ftable td { padding: 2px 4px; border-bottom: 1px solid #131c24; }
.mono { font-family: ui-monospace, monospace; }
.dim { color: #5b6b79; }
.tiny { font-size: 10.5px; }
.ok { color: #39cfbf; }
.bad { color: #e5544b; }
.err { color: #e5544b; font-size: 11px; }
.out {
  margin: 8px 0 0; background: #070b0f; border: 1px solid #141d26; padding: 8px;
  font-family: ui-monospace, monospace; font-size: 10.5px; color: #9fb0be;
  max-height: 150px; overflow: auto; white-space: pre-wrap;
}
</style>
