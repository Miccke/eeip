/** S7 验收：流程图页面 + 编辑器 + 校验 */
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

const setV = async (sel, v) => p.$eval(sel, (el, val) => {
  el.value = val
  el.dispatchEvent(new Event('input', { bubbles: true }))
}, v)

await p.goto(`${BASE}/login`, { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin')
await setV('input[placeholder="GROUP"]', 'GROUP')
await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]')
await p.waitForFunction(() => location.pathname !== '/login', { timeout: 15000 })
await new Promise((r) => setTimeout(r, 1200))

await p.goto(`${BASE}/asset/flow`, { waitUntil: 'networkidle0' })
await new Promise((r) => setTimeout(r, 1200))
await p.screenshot({ path: `${OUT}/s7-flow-list.png` })

// 打开编辑（第一行）
await p.evaluate(() => {
  const a = [...document.querySelectorAll('a')].find((x) => x.textContent.trim() === '编辑')
  if (a) a.click()
})
await new Promise((r) => setTimeout(r, 1800))
await p.screenshot({ path: `${OUT}/s7-flow-editor.png` })

// 点"校验"
await p.evaluate(() => {
  // Ant Design 会在两个汉字之间插空格（"校 验"），匹配时先去掉空白
  const btn = [...document.querySelectorAll('.ant-modal button')]
    .find((x) => x.textContent.replace(/\s/g, '') === '校验')
  if (btn) btn.click()
})
await p.waitForFunction(() => document.body.innerText.includes('FLOW_') || document.body.innerText.includes('校验通过'), { timeout: 8000 }).catch(() => {})
await new Promise((r) => setTimeout(r, 800))
await p.screenshot({ path: `${OUT}/s7-flow-validate.png` })

const body = await p.evaluate(() => document.body.innerText)
console.log(JSON.stringify({
  hasGraph: body.includes('HTTP_INVOKE'),
  hasPreview: body.includes('流程预览') || body.includes('Flow preview'),
  errors
}, null, 2))
await b.close()
