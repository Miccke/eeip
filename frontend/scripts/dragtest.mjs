import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900 } })
const p = await b.newPage()
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)
await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 600))
await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1200))
await p.evaluate(() => [...document.querySelectorAll('.pnode')].find(x => x.textContent.includes('HTTP 调用'))?.click())
await new Promise(r => setTimeout(r, 400))

const before = await p.$eval('.fc-node', el => ({ left: el.style.left, top: el.style.top }))
const rect = await p.$eval('.fc-node', el => { const r = el.getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height } })

const after = await p.evaluate(async (r) => {
  const node = document.querySelector('.fc-node')
  const sx = r.x + r.w / 2, sy = r.y + r.h / 2
  node.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: sx, clientY: sy, buttons: 1 }))
  for (let i = 1; i <= 10; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: sx + i * 20, clientY: sy + i * 12, buttons: 1 }))
    await new Promise(res => setTimeout(res, 20))
  }
  const during = { left: node.style.left, top: node.style.top }
  window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, clientX: sx + 200, clientY: sy + 120 }))
  await new Promise(res => setTimeout(res, 300))
  return { during, dropped: { left: node.style.left, top: node.style.top } }
}, rect)

console.log(JSON.stringify({ before, after }, null, 2))
await b.close()
