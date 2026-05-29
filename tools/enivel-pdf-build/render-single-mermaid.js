// Render a single mermaid file to PNG.
// Usage: node render-single-mermaid.js <input.mmd> <output.png>
const puppeteer = require('puppeteer');
const fs = require('fs');

const renderHtml = code => `<!DOCTYPE html>
<html><head><meta charset="utf-8">
<style>
  body { margin: 0; padding: 24px; font-family: -apple-system, sans-serif; background: white; }
  #d { display: inline-block; }
</style>
</head><body>
<div id="d" class="mermaid">${code}</div>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<script>
  mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
  mermaid.run({ querySelector: '.mermaid' })
    .then(() => { window.__done = true; })
    .catch(e => { console.error(e); window.__done = true; });
</script>
</body></html>`;

(async () => {
  const [, , inputPath, outputPath] = process.argv;
  if (!inputPath || !outputPath) { console.error('Usage: <input.mmd> <output.png>'); process.exit(1); }
  const code = fs.readFileSync(inputPath, 'utf8');
  const html = renderHtml(code);
  const htmlPath = '/tmp/_single_mermaid.html';
  fs.writeFileSync(htmlPath, html);
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  await page.setViewport({ width: 1800, height: 1200, deviceScaleFactor: 2 });
  await page.goto('file://' + htmlPath, { waitUntil: 'networkidle2', timeout: 14000 });
  await page.waitForFunction(() => window.__done === true, { timeout: 14000 });
  const el = await page.$('#d');
  if (!el) { console.error('No rendered element'); process.exit(1); }
  await el.screenshot({ path: outputPath, omitBackground: false });
  console.log('Wrote', outputPath);
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
