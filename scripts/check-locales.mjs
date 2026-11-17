/**
 * 校验三份语言词条的 key 完全一致。
 * 漏翻不该靠肉眼发现 —— CI 直接拦下。
 */
import { readFileSync } from 'node:fs'

const LOCALES = ['zh-CN', 'zh-TW', 'en-US']

/** 逐字符遍历，把字符串字面量替换成 ''（正确处理转义），避免把值里的 "Foo:" 当成 key */
function stripStrings(src) {
  let out = ''
  let i = 0
  let quote = null
  while (i < src.length) {
    const ch = src[i]
    if (quote) {
      if (ch === '\\') { i += 2; continue }
      if (ch === quote) { quote = null; out += ch; i++; continue }
      i++
      continue
    }
    if (ch === "'" || ch === '"' || ch === '`') {
      quote = ch
      out += ch
      i++
      continue
    }
    out += ch
    i++
  }
  return out
}

/** 取出 "分组.键" 列表 */
function keysOf(file) {
  const keys = new Set()
  let group = null
  for (const line of stripStrings(readFileSync(file, 'utf8')).split('\n')) {
    const g = line.match(/^\s{2}([a-zA-Z]+):\s*\{\s*$/)
    if (g) { group = g[1]; continue }
    if (/^\s{2}\},?\s*$/.test(line)) { group = null; continue }
    if (!group) continue
    // 一行可能写多个键（如 colCode / colName 同行），全部收集
    for (const m of line.matchAll(/([a-zA-Z][a-zA-Z0-9]*)\s*:/g)) {
      keys.add(`${group}.${m[1]}`)
    }
  }
  return keys
}

const sets = LOCALES.map((l) => ({ l, keys: keysOf(`frontend/src/locales/${l}.ts`) }))
const base = sets[0]
let failed = false

for (const { l, keys } of sets.slice(1)) {
  const missing = [...base.keys].filter((k) => !keys.has(k))
  const extra = [...keys].filter((k) => !base.keys.has(k))
  if (missing.length || extra.length) {
    failed = true
    console.error(`✗ ${l} 与 ${base.l} 不一致`)
    if (missing.length) console.error(`  缺少: ${missing.join(', ')}`)
    if (extra.length) console.error(`  多余: ${extra.join(', ')}`)
  } else {
    console.log(`✓ ${l}: ${keys.size} 条，与基准一致`)
  }
}

console.log(failed ? '词条校验未通过' : `词条校验通过（${base.keys.size} 条 × ${LOCALES.length} 语言）`)
process.exit(failed ? 1 : 0)
