const puppeteer = require('puppeteer');
const path = require('path');

const SLIDES = 'file://' + path.resolve('/Volumes/Evaka/enivel/presentations/2026-06-tiera-tapaaminen/index.html') + '?print-pdf';
const OUT = '/Volumes/Evaka/enivel/pdf/tiera-tapaaminen-2026-06.pdf';

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  try {
    const page = await browser.newPage();
    await page.goto(SLIDES, { waitUntil: 'networkidle2', timeout: 10000 });
    await page.waitForFunction(() => window.Reveal && window.Reveal.isReady(), { timeout: 10000 });
    await page.pdf({
      path: OUT,
      printBackground: true,
      preferCSSPageSize: true,
      width: '1280px',
      height: '720px',
      margin: { top: 0, right: 0, bottom: 0, left: 0 },
    });
    console.log('Wrote', OUT);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
