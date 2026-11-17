/**
 * 用本机 Chrome 走一遍登录流程并截图，用于验证 RBAC 前端闭环。
 * 用法: node scripts/shot.mjs [outDir]
 */
import puppeteer from 'puppeteer-core'

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const BASE = 'http://localhost:5173'
const OUT = process.argv[2] || '/tmp/eeip-shots'
const USER = process.argv[3] || 'admin'
const PASS = process.argv[4] || 'Admin@123'
const TAG = process.argv[5] || 'admin'
const TENANT = process.argv[6] || 'GROUP'

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--no-sandbox', '--window-size=1440,900'],
  defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 }
})

const page = await browser.newPage()
const errors = []
page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
page.on('pageerror', (e) => errors.push(String(e)))

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle0' })
await page.screenshot({ path: `${OUT}/login.png` })

// 登录（直接赋值并派发 input 事件，避免与已填充内容拼接）
async function setValue(selector, value) {
  await page.$eval(selector, (el, v) => {
    el.value = v
    el.dispatchEvent(new Event('input', { bubbles: true }))
  }, value)
}
await setValue('input[placeholder="用户名"]', USER)
await setValue('input[placeholder="GROUP"]', TENANT)
await setValue('input[placeholder="密码"]', PASS)
await page.click('button[type=submit]')
await page.waitForFunction(() => location.pathname !== '/login', { timeout: 15000 })
await new Promise((r) => setTimeout(r, 2600))
await page.screenshot({ path: `${OUT}/dashboard-${TAG}.png` })

// 用户管理
await page.goto(`${BASE}/system/user`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 900))
await page.screenshot({ path: `${OUT}/users-${TAG}.png` })

const bodyText = await page.evaluate(() => document.body.innerText)
const hasPermBtn = bodyText.includes('新增用户')
const finalUrl = page.url()
const redirected = new URL(finalUrl).pathname !== '/system/user'

// 角色管理
await page.goto(`${BASE}/system/role`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 900))
await page.screenshot({ path: `${OUT}/roles-${TAG}.png` })

// 租户管理
await page.goto(`${BASE}/system/tenant`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 900))
await page.screenshot({ path: `${OUT}/tenants-${TAG}.png` })

// 审计日志
const userCount = await page.evaluate(() => {
  const el = document.querySelector('.ant-pagination-total-text')
  return el ? el.textContent : null
})
await page.goto(`${BASE}/ops/audit`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 1200))
await page.screenshot({ path: `${OUT}/audit-${TAG}.png` })

console.log(JSON.stringify({ user: USER, hasPermBtn, systemUserUrl: finalUrl, redirected, userCount, errors }, null, 2))
await browser.close()
