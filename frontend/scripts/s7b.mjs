/** 设计器 1:1 验收 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', (e) => errors.push(String(e)))
p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)
const clickText = (txt) => p.evaluate((t) => {
  const el = [...document.querySelectorAll('a,button,.tab,.ctab')]
    .find((x) => x.textContent.replace(/\s/g, '') === t)
  if (el) { el.click(); return true }
  return false
}, txt)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 900))

const id = await p.evaluate(async () => {
  const r = await fetch('/api/v1/integration/flows', { headers: { Authorization: 'Bearer ' + localStorage.getItem('eeip_token') } })
  const j = await r.json(); return j.data?.[0]?.id
})
await p.goto(`http://localhost:5173/asset/flow/${id}`, { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1800))
await p.screenshot({ path: '/tmp/eeip-shots/s7-designer.png' })

// 选中第二个节点（HTTP_INVOKE）
await p.evaluate(() => {
  const n = document.querySelectorAll('.node')[1]
  n?.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: 0, clientY: 0 }))
})
await new Promise(r => setTimeout(r, 700))
await p.screenshot({ path: '/tmp/eeip-shots/s7-designer-panel.png' })

await clickText('校验')
await new Promise(r => setTimeout(r, 1600))
await p.screenshot({ path: '/tmp/eeip-shots/s7-designer-validate.png' })

console.log(JSON.stringify({ nodes: await p.$$eval('.node', e => e.length), errors: errors.slice(0, 3) }, null, 2))
await b.close()
