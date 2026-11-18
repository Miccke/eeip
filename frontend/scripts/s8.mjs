/** S8 验收：调色板来自后端 manifest */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
const requests = []
p.on('pageerror', (e) => errors.push(String(e)))
p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
p.on('request', (r) => { if (r.url().includes('node-types')) requests.push(r.url()) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 700))

await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1600))
await p.screenshot({ path: '/tmp/eeip-shots/s8-palette.png' })

const palette = await p.$$eval('.palette .pnode', els => els.map(e => e.textContent.trim().replace(/\s+/g, ' ')))
const groups = await p.$$eval('.palette .palette__g', els => els.map(e => e.textContent.trim()))
console.log(JSON.stringify({ nodeTypesRequested: requests.length, groups, palette, errors: errors.slice(0, 3) }, null, 2))
await b.close()
