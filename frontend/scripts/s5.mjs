/** S5 验收：租户页（一键开通入口）+ 连接管理页 + 连通性测试 */
import puppeteer from 'puppeteer-core'
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const BASE = 'http://localhost:5173'
const OUT = '/tmp/eeip-shots'

const b = await puppeteer.launch({
  executablePath: CHROME, headless: 'new', args: ['--no-sandbox'],
  defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 }
})
const p = await b.newPage()
const errors = []
p.on('pageerror', (e) => errors.push(String(e)))
p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })

async function setValue(sel, v) {
  await p.$eval(sel, (el, val) => {
    el.value = val
    el.dispatchEvent(new Event('input', { bubbles: true }))
  }, v)
}

await p.goto(`${BASE}/login`, { waitUntil: 'networkidle0' })
await setValue('input[placeholder="用户名"]', 'admin')
await setValue('input[placeholder="GROUP"]', 'GROUP')
await setValue('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]')
await p.waitForFunction(() => location.pathname !== '/login', { timeout: 15000 })
await new Promise((r) => setTimeout(r, 800))

// 连接管理页
await p.goto(`${BASE}/asset/connection`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 1200))
await p.screenshot({ path: `${OUT}/s5-connection.png` })

// 点第一个"测试"
const clicked = await p.evaluate(() => {
  const links = [...document.querySelectorAll('a')]
  const t = links.find((a) => a.textContent.trim().startsWith('测试'))
  if (t) { t.click(); return true }
  return false
})
await new Promise((r) => setTimeout(r, 3000))
await p.screenshot({ path: `${OUT}/s5-connection-test.png` })

// 租户页
await p.goto(`${BASE}/system/tenant`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 900))
await p.screenshot({ path: `${OUT}/s5-tenant.png` })

const body = await p.evaluate(() => document.body.innerText)
console.log(JSON.stringify({
  clickedTest: clicked,
  hasProvisionBtn: body.includes('一键开通租户'),
  errors
}, null, 2))
await b.close()
