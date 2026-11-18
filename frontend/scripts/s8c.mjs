/** 验证：不同流程显示不同内容；保存后重进仍绑定 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', (e) => errors.push(String(e)))
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 700))

const flows = await p.evaluate(async () => {
  const r = await fetch('/api/v1/integration/flows', { headers: { Authorization: 'Bearer ' + localStorage.getItem('eeip_token') } })
  const j = await r.json(); return j.data.map(f => ({ id: f.id, code: f.code, nodes: f.nodeCount }))
})
console.log('远端流程:', JSON.stringify(flows))

const snap = []
for (const f of flows.slice(0, 2)) {
  await p.goto(`http://localhost:5173/asset/flow/${f.id}`, { waitUntil: 'networkidle0' })
  await new Promise(r => setTimeout(r, 1300))
  const info = await p.evaluate(() => ({
    name: document.querySelector('.dz-bar__name')?.textContent.trim(),
    nodes: document.querySelectorAll('.fc-node').length,
    types: [...document.querySelectorAll('.fc-node__t')].map(e => e.textContent.trim())
  }))
  snap.push({ expected: f, got: info })
}
await p.screenshot({ path: '/tmp/eeip-shots/s8c-bound.png' })
console.log(JSON.stringify({ snap, errors: errors.slice(0, 2) }, null, 2))
await b.close()
