import puppeteer from 'puppeteer-core'
const CHROME='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const b = await puppeteer.launch({executablePath: CHROME, headless:'new', args:['--no-sandbox'], defaultViewport:{width:1440,height:900}})
const p = await b.newPage()
p.on('pageerror', e => console.log('[pageerror]', String(e).slice(0,300)))
p.on('response', async r => { if (r.url().includes('/api/')) console.log('[api]', r.status(), r.url()) })
await p.goto('http://localhost:5173/login', {waitUntil:'networkidle0'})
async function setValue(sel, v){ await p.$eval(sel,(el,val)=>{el.value=val; el.dispatchEvent(new Event('input',{bubbles:true}))}, v) }
await setValue('input[placeholder="用户名"]','admin')
await setValue('input[placeholder="密码"]','Admin@123')
console.log('values:', await p.$$eval('input', els=>els.map(e=>e.value)))
await p.click('button[type=submit]')
await new Promise(r=>setTimeout(r,4000))
console.log('url:', p.url())
console.log('token:', await p.evaluate(()=>localStorage.getItem('eeip_token')?.slice(0,20)))
console.log('body:', (await p.evaluate(()=>document.body.innerText)).slice(0,160).replace(/\n/g,' | '))
await b.close()
