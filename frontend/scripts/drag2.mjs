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

const res = await p.evaluate(async () => {
  const node = document.querySelector('.fc-node')
  const r = node.getBoundingClientRect()
  const sx = r.x + 20, sy = r.y + 20
  // 真实鼠标：先按下、移动、在节点内松开
  node.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: sx, clientY: sy, buttons: 1 }))
  for (let i = 1; i <= 6; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: sx + i * 15, clientY: sy + i * 8, buttons: 1 }))
    await new Promise(r => setTimeout(r, 15))
  }
  const during = { left: node.style.left, top: node.style.top }
  node.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, clientX: sx + 90, clientY: sy + 48 }))
  // 松手后再移动鼠标 200px，节点不应再跟随
  for (let i = 1; i <= 5; i++) {
    window.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, clientX: sx + 200 + i * 20, clientY: sy + 200 + i * 20 }))
    await new Promise(r => setTimeout(r, 15))
  }
  return { during, after: { left: node.style.left, top: node.style.top } }
})
console.log(JSON.stringify(res, null, 2))
await b.close()
