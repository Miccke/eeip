/** 验收：API 资产页 + 拖动落位 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', e => errors.push(String(e)))
p.on('console', m => { if (m.type() === 'error') errors.push(m.text().slice(0, 200)) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 700))

// ① 拖动落位（普通模式）
await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1300))
await p.evaluate(() => [...document.querySelectorAll('.pnode')].find(x => x.textContent.includes('HTTP 调用'))?.click())
await new Promise(r => setTimeout(r, 400))
const drag = await p.evaluate(async () => {
  const node = document.querySelector('.fc-node')
  const r = node.getBoundingClientRect()
  const before = { left: node.style.left, top: node.style.top }
  const sx = r.x + 20, sy = r.y + 20
  node.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: sx, clientY: sy, buttons: 1 }))
  for (let i = 1; i <= 8; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: sx + i * 18, clientY: sy + i * 10, buttons: 1 }))
    await new Promise(r => setTimeout(r, 15))
  }
  const during = { left: node.style.left, top: node.style.top }
  window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true }))
  await new Promise(r => setTimeout(r, 250))
  return { before, during, after: { left: node.style.left, top: node.style.top } }
})

// ② API 资产页
await p.goto('http://localhost:5173/asset/api', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1200))
await p.screenshot({ path: '/tmp/eeip-shots/s9-api-assets.png' })
const apiRows = await p.$$eval('tbody tr', els => els.length)

console.log(JSON.stringify({ drag, apiRows, errors: errors.slice(0, 2) }, null, 2))
await b.close()
