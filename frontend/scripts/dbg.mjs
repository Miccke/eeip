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
await new Promise(r => setTimeout(r, 900))
const fid = await p.evaluate(async () => {
  const r = await fetch('/api/v1/integration/flows', { headers: { Authorization: 'Bearer ' + localStorage.getItem('eeip_token') } })
  const j = await r.json(); return j.data.find(f => f.code === 'demo-run-order')?.id
})
await p.goto('http://localhost:5173/asset/flow/' + fid, { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1800))
const nodes = await p.$$('.fc-node')
await nodes[2].click()
await new Promise(r => setTimeout(r, 600))
// 点「编辑映射」
const btns = await p.$$('button')
for (const btn of btns) {
  const txt = await (await btn.getProperty('innerText')).jsonValue()
  if (txt.includes('编辑映射')) { await btn.click(); break }
}
await new Promise(r => setTimeout(r, 1500))
await p.screenshot({ path: '/tmp/eeip-shots/s19-mapping.png' })
const modal = await p.evaluate(() => {
  const m = document.querySelector('.ant-modal-body')
  return m ? m.innerText.replace(/\n+/g, ' | ').slice(0, 420) : 'NO_MODAL'
})
console.log(JSON.stringify({ modal, errors: errors.slice(0, 2) }, null, 2))
await b.close()
