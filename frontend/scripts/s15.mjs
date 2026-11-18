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
await new Promise(r => setTimeout(r, 800))
for (const [path, name] of [['/asset/api', 's15-api'], ['/asset/domain', 's15-domain'], ['/asset/system', 's15-system']]) {
  await p.goto('http://localhost:5173' + path, { waitUntil: 'networkidle0' })
  await new Promise(r => setTimeout(r, 1200))
  await p.screenshot({ path: `/tmp/eeip-shots/${name}.png` })
}
console.log(JSON.stringify({ errors: errors.slice(0, 3) }, null, 2))
await b.close()
