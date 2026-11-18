/** 第二轮：框选、拾取模式、新建空白、流程绑定 */
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
await new Promise(r => setTimeout(r, 700))

// 新建：应为空白画布
await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1500))
const emptyState = await p.$$eval('.fc-node', e => e.length)
await p.screenshot({ path: '/tmp/eeip-shots/s8b-empty.png' })

// 加 3 个节点
for (const n of ['HTTP调用', '数据映射', '延时']) { await clickText(n); await new Promise(r => setTimeout(r, 250)) }
const afterAdd = await p.$$eval('.fc-node', e => e.length)

// 框选：从画布左上拖到右下
const marquee = await p.evaluate(async () => {
  const board = document.querySelector('.fc-board')
  const r = board.getBoundingClientRect()
  const pt = (x, y) => ({ clientX: r.x + x, clientY: r.y + y })
  board.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, ...pt(20, 20) }))
  for (let i = 1; i <= 6; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, ...pt(20 + i * 120, 20 + i * 90) }))
    await new Promise(r => setTimeout(r, 25))
  }
  window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true }))
  await new Promise(r => setTimeout(r, 300))
  return document.querySelectorAll('.fc-node.is-sel').length
})
await p.screenshot({ path: '/tmp/eeip-shots/s8b-marquee.png' })

// 对齐按钮（左对齐）
await p.evaluate(() => document.querySelectorAll('.dz-tool')[0]?.click())
await new Promise(r => setTimeout(r, 300))
const lefts = await p.$$eval('.fc-node.is-sel', els => els.map(e => Math.round(e.getBoundingClientRect().left)))
await p.screenshot({ path: '/tmp/eeip-shots/s8b-align.png' })

// 拾取模式：开启后点节点应"拿起"并跟随
await p.evaluate(() => {
  const btns = [...document.querySelectorAll('.dz-tool')]
  btns[btns.length - 1].click()
})
await new Promise(r => setTimeout(r, 200))
const carry = await p.evaluate(async () => {
  const node = document.querySelector('.fc-node')
  const r = node.getBoundingClientRect()
  node.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: r.x + 10, clientY: r.y + 10 }))
  await new Promise(r => setTimeout(r, 100))
  window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: r.x + 160, clientY: r.y + 60 }))
  await new Promise(r => setTimeout(r, 200))
  return document.querySelectorAll('.fc-node.is-carry').length
})

console.log(JSON.stringify({ emptyState, afterAdd, marqueeSelected: marquee, alignedLefts: lefts, carrying: carry, errors: errors.slice(0, 3) }, null, 2))
await b.close()
