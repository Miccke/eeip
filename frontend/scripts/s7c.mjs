/** 设计器交互：新建流程 → 调色板加节点 → 端口连线 → 校验 */
import puppeteer from 'puppeteer-core'
const b = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: 'new', args: ['--no-sandbox'], defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 2 } })
const p = await b.newPage()
const errors = []
p.on('pageerror', (e) => errors.push(String(e)))
p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
const setV = async (sel, v) => p.$eval(sel, (el, val) => { el.value = val; el.dispatchEvent(new Event('input', { bubbles: true })) }, v)
const clickText = (txt) => p.evaluate((t) => {
  const el = [...document.querySelectorAll('a,button,.tab,.ctab,.pnode')]
    .find((x) => x.textContent.replace(/\s/g, '') === t)
  if (el) { el.click(); return true } return false
}, txt)

await p.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' })
await setV('input[placeholder="用户名"]', 'admin'); await setV('input[placeholder="GROUP"]', 'GROUP'); await setV('input[placeholder="密码"]', 'Admin@123')
await p.click('button[type=submit]'); await p.waitForFunction(() => location.pathname !== '/login')
await new Promise(r => setTimeout(r, 800))

await p.goto('http://localhost:5173/asset/flow/new', { waitUntil: 'networkidle0' })
await new Promise(r => setTimeout(r, 1200))
const before = await p.$$eval('.node', e => e.length)

// 调色板加两个节点
await clickText('HTTP调用'); await new Promise(r => setTimeout(r, 350))
await clickText('数据映射'); await new Promise(r => setTimeout(r, 350))
const after = await p.$$eval('.node', e => e.length)

// 从第 1 个节点的右侧端口拖到第 2 个节点（模拟 pointer 事件）
await p.evaluate(() => {
  const nodes = [...document.querySelectorAll('.node')]
  const port = nodes[0]?.querySelector('.node__port--r')
  if (!port || !nodes[1]) return
  const r = port.getBoundingClientRect()
  port.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: r.x + 3, clientY: r.y + 3 }))
  nodes[1].dispatchEvent(new PointerEvent('pointerup', { bubbles: true }))
})
await new Promise(r => setTimeout(r, 600))

const edgeCount = await p.$$eval('svg path[marker-end]', e => e.length)
await p.screenshot({ path: '/tmp/eeip-shots/s7-designer-new.png' })

await clickText('校验')
await new Promise(r => setTimeout(r, 1600))
await p.screenshot({ path: '/tmp/eeip-shots/s7-designer-new-validate.png' })

const body = await p.evaluate(() => document.body.innerText)
console.log(JSON.stringify({
  nodesBefore: before, nodesAfter: after, edges: edgeCount,
  validationShown: body.includes('valid'),
  errors: errors.slice(0, 3)
}, null, 2))
await b.close()
