const puppeteer = require('puppeteer');
const path = require('path');

const PAGE = 'file://' + path.resolve('/tmp/enivel-pdf-build/idm-api-arrows.html');
const OUT = '/Volumes/Evaka/enivel/pdf/IdM-API-liittyvat-dokumentaatiot-nuolet.png';

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1320, height: 320, deviceScaleFactor: 2 });
    await page.goto(PAGE, { waitUntil: 'networkidle2', timeout: 10000 });
    await page.evaluate(() => document.fonts && document.fonts.ready);
    await page.screenshot({ path: OUT, type: 'png', fullPage: false, clip: { x: 0, y: 0, width: 1320, height: 320 } });
    console.log('Wrote', OUT);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
