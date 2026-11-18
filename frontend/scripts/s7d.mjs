/** 设计器第二轮改造验收：拖拽落位、连线、对齐、调色板可点 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', (e) => errors.push(String(e)))
p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)
const clickText = (txt) => p.evaluate((t) => {
  const el = [...document.querySelectorAll('a,button,.tab,.ctab,.pnode')]
    .find((x) => x.textContent.replace(/\s/g, '') === t)
  if (el) { el.click(); return true } return false
}, txt)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 800))

await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1200))

// 调色板点 4 个不同类型的节点
for (const name of ['HTTP调用', '数据映射', '条件分支', '延时']) {
  await clickText(name); await new Promise(r => setTimeout(r, 300))
}
const afterAdd = await p.$$eval('.fc-node', e => e.length)

// 拖动第一个节点到 (200, 420)，松开后应停在原地
const drag = await p.evaluate(async () => {
  const el = document.querySelector('.fc-node')
  const r = el.getBoundingClientRect()
  const start = { x: r.x + 40, y: r.y + 20 }
  el.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: start.x, clientY: start.y }))
  for (let i = 1; i <= 8; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: start.x, clientY: start.y + i * 20 }))
    await new Promise(r => setTimeout(r, 20))
  }
  window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true }))
  await new Promise(r => setTimeout(r, 300))
  const after = el.getBoundingClientRect()
  return { moved: Math.round(after.y - r.y) }
})

// 鼠标再移动 100px，节点不应跟随
const follow = await p.evaluate(async () => {
  const el = document.querySelector('.fc-node')
  const before = el.getBoundingClientRect().top
  window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: 600, clientY: 700 }))
  await new Promise(r => setTimeout(r, 200))
  return Math.round(el.getBoundingClientRect().top - before)
})

// 点击端口 → 点击目标节点完成连线（点击模式）
const link = await p.evaluate(async () => {
  const nodes = [...document.querySelectorAll('.fc-node')]
  const src = nodes.find((n) => n.querySelector('.fc-port--r'))
  const port = src?.querySelector('.fc-port--r')
  if (!port) return { ok: false, reason: 'no port' }
  const target = nodes.find((n) => n !== src && n.querySelector('.fc-port--l'))
  port.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
  await new Promise(r => setTimeout(r, 150))
  target?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
  await new Promise(r => setTimeout(r, 300))
  return { ok: true, edges: document.querySelectorAll('.fc-edge').length }
})

await p.screenshot({ path: '/tmp/eeip-shots/s7d-designer.png' })

// 多选 + 对齐
await p.evaluate(() => {
  const nodes = [...document.querySelectorAll('.fc-node')]
  nodes[1].dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, shiftKey: true }))
  nodes[2].dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, shiftKey: true }))
})
await new Promise(r => setTimeout(r, 300))
const alignBtn = await p.evaluate(() => {
  const btn = [...document.querySelectorAll('.dz-tool')][0]
  if (btn) { btn.click(); return true } return false
})
await new Promise(r => setTimeout(r, 400))
await p.screenshot({ path: '/tmp/eeip-shots/s7d-align.png' })

console.log(JSON.stringify({ afterAdd, drag, followDrift: follow, link, alignBtn, errors: errors.slice(0, 3) }, null, 2))
await b.close()
