/**
 * 节点规格同步：前端为唯一来源，构建时拷贝给后端做校验/执行。
 *
 * 为什么这样：UI 描述（图标/颜色/尺寸/表单）天然属于前端；
 * 但 config schema 与端口又必须被后端校验使用。折中做法是
 * "源码放前端、构建期同步到后端资源"，避免两边各存一份产生漂移。
 *
 * 用法：node scripts/sync-node-packages.mjs [--check]
 *   --check 只比对不写入，CI 用它防漂移
 */
import { cpSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync } from 'node:fs'
import { join } from 'node:path'

const SRC = 'frontend/src/node-packages'
const DST = 'backend/src/main/resources/node-packages'
const checkOnly = process.argv.includes('--check')

if (!existsSync(SRC)) {
  console.error(`✗ 源目录不存在：${SRC}`)
  process.exit(1)
}

const pkgs = readdirSync(SRC).filter((d) => existsSync(join(SRC, d, 'node.json')))

if (checkOnly) {
  let drift = 0
  for (const p of pkgs) {
    const a = join(SRC, p, 'node.json')
    const b = join(DST, p, 'node.json')
    if (!existsSync(b)) { console.error(`✗ 后端缺少 ${p}/node.json`); drift++; continue }
    if (readFileSync(a, 'utf-8') !== readFileSync(b, 'utf-8')) {
      console.error(`✗ ${p}/node.json 与后端不一致`); drift++
    }
  }
  if (drift) {
    console.error(`\n发现 ${drift} 处漂移，请运行：node scripts/sync-node-packages.mjs`)
    process.exit(1)
  }
  console.log(`✓ 节点规格一致（${pkgs.length} 个）`)
  process.exit(0)
}

rmSync(DST, { recursive: true, force: true })
mkdirSync(DST, { recursive: true })
for (const p of pkgs) cpSync(join(SRC, p), join(DST, p), { recursive: true })
console.log(`✓ 已同步 ${pkgs.length} 个节点规格到后端资源`)
