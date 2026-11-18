/** 验收：调试按钮真跑流程 + 节点轨迹 */
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
const fid = await p.evaluate(async () => {
  const r = await fetch('/api/v1/integration/flows', { headers: { Authorization: 'Bearer ' + localStorage.getItem('eeip_token') } })
  const j = await r.json()
  return j.data.find(f => f.code === 'demo-run-order')?.id
})
await p.goto(`http://localhost:5173/asset/flow/${fid}`, { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1500))
await p.evaluate(() => [...document.querySelectorAll('button')].find(x => x.textContent.replace(/\s/g, '') === '调试')?.click())
await new Promise(r => setTimeout(r, 3000))
await p.screenshot({ path: '/tmp/eeip-shots/s13-run.png' })
const consoleText = await p.evaluate(() => document.querySelector('.console__body')?.innerText || '')
console.log(JSON.stringify({
  hasTrace: consoleText.includes('SUCCESS'),
  head: consoleText.split('\n').slice(0, 7),
  errors: errors.slice(0, 2)
}, null, 2))
await b.close()
