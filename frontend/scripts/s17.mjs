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
await new Promise(r => setTimeout(r, 900))
await p.goto('http://localhost:5173/ops/instances', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1400))
await p.screenshot({ path: '/tmp/eeip-shots/s17-instances.png' })
const listText = await p.evaluate(() => document.body.innerText.slice(0, 400))

// 打开第一条详情
await p.evaluate(() => {
  const a = [...document.querySelectorAll('tbody a')].find(x => x.textContent.includes('详情'))
  a?.click()
})
await new Promise(r => setTimeout(r, 1500))
await p.screenshot({ path: '/tmp/eeip-shots/s17-trace.png' })
const drawerText = await p.evaluate(() => {
  const d = document.querySelector('.ant-drawer-body')
  return d ? d.innerText.slice(0, 300) : 'NO_DRAWER'
})
console.log(JSON.stringify({ listText: listText.slice(0, 160), drawer: drawerText.slice(0, 200), errors: errors.slice(0, 2) }, null, 2))
await b.close()
