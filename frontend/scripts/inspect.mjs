import puppeteer from 'puppeteer-core'
const CHROME='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const b = await puppeteer.launch({executablePath: CHROME, headless:'new', args:['--no-sandbox'], defaultViewport:{width:1440,height:900}})
const p = await b.newPage()
await p.goto('http://localhost:5173/login', {waitUntil:'networkidle0'})
async function setValue(sel, v){ await p.$eval(sel,(el,val)=>{el.value=val; el.dispatchEvent(new Event('input',{bubbles:true}))}, v) }
await setValue('input[placeholder="用户名"]','admin')
await setValue('input[placeholder="密码"]','Admin@123')
await p.click('button[type=submit]')
await p.waitForFunction(()=>location.pathname!=='/login',{timeout:15000})
await new Promise(r=>setTimeout(r,1500))
const info = await p.evaluate(() => {
  const at = document.elementFromPoint(787, 25)
  const sider = document.querySelector('.ant-layout-sider')
  const menu = document.querySelector('.ant-menu-dark')
  return {
    atPoint: at ? (at.className || at.tagName) + ' | ' + (at.textContent||'').slice(0,40) : null,
    siderBg: sider ? getComputedStyle(sider).backgroundColor : null,
    menuBg: menu ? getComputedStyle(menu).backgroundColor : null,
    contentBg: getComputedStyle(document.querySelector('.ant-layout-content')).backgroundColor
  }
})
console.log(JSON.stringify(info, null, 2))
await b.close()
