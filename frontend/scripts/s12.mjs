/** 验收：连线删除按钮、框选后批量删除、Ctrl+C/V、右键菜单 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', e => errors.push(String(e)))
p.on('console', m => { if (m.type() === 'error') errors.push(m.text().slice(0, 160)) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)
const clickPalette = (name) => p.evaluate((n) => {
  [...document.querySelectorAll('.pnode')].find(x => x.textContent.includes(n))?.click()
}, name)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 700))
await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1200))

// 加两个节点并连线
await clickPalette('HTTP 调用'); await new Promise(r => setTimeout(r, 300))
await clickPalette('数据映射'); await new Promise(r => setTimeout(r, 300))
const linkRes = await p.evaluate(async () => {
  const ns = [...document.querySelectorAll('.fc-node')]
  const port = ns[0].querySelector('.fc-port--r')
  port.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
  await new Promise(r => setTimeout(r, 100))
  ns[1].dispatchEvent(new MouseEvent('click', { bubbles: true }))
  await new Promise(r => setTimeout(r, 300))
  return document.querySelectorAll('.fc-edge').length
})

// 悬停连线 → 删除按钮出现 → 点击删除
const delRes = await p.evaluate(async () => {
  const edge = document.querySelector('.fc-edge')
  const r = edge.getBoundingClientRect()
  edge.dispatchEvent(new PointerEvent('pointerenter', { bubbles: true }))
  await new Promise(r => setTimeout(r, 200))
  const btn = document.querySelector('.fc-edge-del')
  if (!btn) return { shown: false }
  btn.click()
  await new Promise(r => setTimeout(r, 300))
  return { shown: true, edgesAfter: document.querySelectorAll('.fc-edge').length }
})

// 重新连线，框选两个节点 → Ctrl+C / Ctrl+V
await p.evaluate(async () => {
  const ns = [...document.querySelectorAll('.fc-node')]
  const port = ns[0].querySelector('.fc-port--r')
  port.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
  await new Promise(r => setTimeout(r, 80))
  ns[1].dispatchEvent(new MouseEvent('click', { bubbles: true }))
})
await new Promise(r => setTimeout(r, 300))
const pasteRes = await p.evaluate(async () => {
  const board = document.querySelector('.fc-board')
  const r = board.getBoundingClientRect()
  board.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: r.x + 10, clientY: r.y + 10 }))
  for (let i = 1; i <= 5; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: r.x + 10 + i * 60, clientY: r.y + 10 + i * 40 }))
    await new Promise(res => setTimeout(res, 20))
  }
  window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true }))
  await new Promise(res => setTimeout(res, 300))
  const selected = document.querySelectorAll('.fc-node.is-sel').length
  window.dispatchEvent(new KeyboardEvent('keydown', { key: 'c', metaKey: true, bubbles: true }))
  await new Promise(res => setTimeout(res, 200))
  window.dispatchEvent(new KeyboardEvent('keydown', { key: 'v', metaKey: true, bubbles: true }))
  await new Promise(res => setTimeout(res, 400))
  return { selected, nodesAfterPaste: document.querySelectorAll('.fc-node').length }
})

// 右键菜单 → 删除选中
const ctxRes = await p.evaluate(async () => {
  const node = document.querySelector('.fc-node')
  const r = node.getBoundingClientRect()
  node.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, clientX: r.x + 10, clientY: r.y + 10 }))
  await new Promise(res => setTimeout(res, 300))
  const menu = document.querySelector('.ctx-menu')
  if (!menu) return { shown: false }
  const before = document.querySelectorAll('.fc-node').length
  const del = [...menu.querySelectorAll('.ctx-item')].find(i => i.textContent.includes('删除'))
  del?.click()
  await new Promise(res => setTimeout(res, 300))
  return { shown: true, before, after: document.querySelectorAll('.fc-node').length }
})
await p.screenshot({ path: '/tmp/eeip-shots/s12-edit.png' })
console.log(JSON.stringify({ linkRes, delRes, pasteRes, ctxRes, errors: errors.slice(0, 2) }, null, 2))
await b.close()
