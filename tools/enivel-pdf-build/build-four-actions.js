const puppeteer = require('puppeteer');
const path = require('path');

const PAGE = 'file://' + path.resolve('/tmp/enivel-pdf-build/four-actions.html');
const OUT = '/Volumes/Evaka/enivel/pdf/ehdotus-seuraavaksi-2026-05-28.png';

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1600, height: 1200, deviceScaleFactor: 2 });
    await page.goto(PAGE, { waitUntil: 'networkidle2', timeout: 10000 });
    // Anna fonttien latautua
    await page.evaluate(() => document.fonts && document.fonts.ready);
    const height = await page.evaluate(() => document.documentElement.scrollHeight);
    await page.setViewport({ width: 1600, height: Math.max(1200, height), deviceScaleFactor: 2 });
    await page.screenshot({ path: OUT, type: 'png', fullPage: true });
    console.log('Wrote', OUT, 'height', height);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
