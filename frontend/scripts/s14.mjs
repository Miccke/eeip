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
await new Promise(r => setTimeout(r, 800))

await p.goto('http://localhost:5173/asset/system', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1100))
await p.screenshot({ path: '/tmp/eeip-shots/s14-systems.png' })
const sysRows = await p.$$eval('tbody tr', els => els.map(e => e.innerText.replace(/\s+/g, ' ').slice(0, 70)))

await p.goto('http://localhost:5173/asset/api', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1100))
await p.screenshot({ path: '/tmp/eeip-shots/s14-ledger.png' })
const apiRows = await p.$$eval('tbody tr', els => els.map(e => e.innerText.replace(/\s+/g, ' ').slice(0, 90)))

const menus = await p.$$eval('.ant-menu-item, .ant-menu-submenu-title', els => els.map(e => e.textContent.trim()))
console.log(JSON.stringify({ sysRows, apiRows, menus: menus.filter(m => m), errors: errors.slice(0, 2) }, null, 2))
await b.close()
