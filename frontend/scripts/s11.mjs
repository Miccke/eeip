/** 验收：输入输出页签 + 连线选中 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', e => errors.push(String(e)))
p.on('console', m => { if (m.type() === 'error') errors.push(m.text().slice(0, 160)) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 700))

const flows = await p.evaluate(async () => {
  const r = await fetch('/api/v1/integration/flows', { headers: { Authorization: 'Bearer ' + localStorage.getItem('eeip_token') } })
  const j = await r.json(); return j.data.map(f => f.id)
})
await p.goto(`http://localhost:5173/asset/flow/${flows[0]}`, { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1500))

// 选中第二个节点，切到"输入"页签
await p.evaluate(() => {
  const nodes = [...document.querySelectorAll('.fc-node')]
  nodes[1]?.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
})
await new Promise(r => setTimeout(r, 400))
await p.evaluate(() => [...document.querySelectorAll('.tab')].find(t => t.textContent.includes('输入'))?.click())
await new Promise(r => setTimeout(r, 500))
await p.screenshot({ path: '/tmp/eeip-shots/s11-io.png' })

// 点击连线（选中），看删除按钮是否常显
const edgeSel = await p.evaluate(async () => {
  const edge = document.querySelector('.fc-edge')
  if (!edge) return { ok: false }
  edge.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
  await new Promise(r => setTimeout(r, 200))
  const del = document.querySelectorAll('.fc-edge-del').length
  return { ok: true, deleteBtnShown: del > 0 }
})

const body = await p.evaluate(() => document.body.innerText)
console.log(JSON.stringify({ panelHasInput: body.includes('上游节点') || body.includes('输入来源'), edgeSel, errors: errors.slice(0, 2) }, null, 2))
await b.close()
