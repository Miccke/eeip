/** 三种语言各截一张用户管理页，验证前端 i18n + 后端菜单/错误文案 */
import puppeteer from 'puppeteer-core'
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const BASE = 'http://localhost:5173'
const OUT = '/tmp/eeip-shots'

const b = await puppeteer.launch({
  executablePath: CHROME, headless: 'new', args: ['--no-sandbox'],
  defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 }
})

for (const lang of ['zh-CN', 'zh-TW', 'en-US']) {
  const p = await b.newPage()
  const errors = []
  p.on('pageerror', (e) => errors.push(String(e)))
  await p.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await p.evaluate((l) => localStorage.setItem('eeip_locale', l), lang)
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle0' })
  await p.screenshot({ path: `${OUT}/i18n-login-${lang}.png` })

  await p.type('input[placeholder]', '') // 触发一次交互，避免有些环境不渲染
  const setV = async (sel, v) => {
    await p.$eval(sel, (el, val) => {
      el.value = val
      el.dispatchEvent(new Event('input', { bubbles: true }))
    }, v)
  }
  // 登录页输入框顺序：用户名 / 租户 / 密码
  const inputs = await p.$$('input')
  await p.evaluate(() => window.scrollTo(0, 0))
  const fill = async (idx, val) => {
    const els = await p.$$('input')
    await els[idx].click({ clickCount: 3 })
    await els[idx].type(val)
  }
  await fill(0, 'admin')
  await fill(1, 'GROUP')
  await fill(2, 'Admin@123')
  await p.click('button[type=submit]')
  await p.waitForFunction(() => location.pathname !== '/login', { timeout: 15000 })
  await new Promise((r) => setTimeout(r, 1500))

  await p.goto(`${BASE}/system/user`, { waitUntil: 'networkidle0' })
  await new Promise((r) => setTimeout(r, 1000))
  await p.screenshot({ path: `${OUT}/i18n-user-${lang}.png` })

  const body = await p.evaluate(() => document.body.innerText)
  console.log(lang, '| 菜单含原文?', body.includes('用户管理') || body.includes('Users') || body.includes('使用者管理'),
    '| 错误:', errors.length)
  await p.close()
}
await b.close()
