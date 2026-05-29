// Render mermaid blocks from given markdown files to PNG images.
// Usage: node render-mermaid-to-png.js <input.md> [input2.md ...]
// Output: <input>-mermaid-<index>.png next to input + manifest.json mapping
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const OUT_DIR = '/tmp/enivel-docx/mermaid';
fs.mkdirSync(OUT_DIR, { recursive: true });

function extractMermaidBlocks(md) {
  const re = /```mermaid\n([\s\S]*?)```/g;
  const out = [];
  let m;
  while ((m = re.exec(md)) !== null) {
    out.push(m[1]);
  }
  return out;
}

const renderHtml = code => `<!DOCTYPE html>
<html><head><meta charset="utf-8">
<style>
  body { margin: 0; padding: 16px; font-family: -apple-system, sans-serif; background: white; }
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
  const inputs = process.argv.slice(2);
  if (!inputs.length) { console.error('No inputs'); process.exit(1); }
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  const manifest = {};
  for (const inputPath of inputs) {
    const md = fs.readFileSync(inputPath, 'utf8');
    const blocks = extractMermaidBlocks(md);
    const basename = path.basename(inputPath, '.md');
    manifest[basename] = [];
    for (let i = 0; i < blocks.length; i++) {
      const code = blocks[i];
      const hash = crypto.createHash('sha1').update(code).digest('hex').slice(0, 10);
      const outName = `${basename}-${i}-${hash}.png`;
      const outPath = path.join(OUT_DIR, outName);
      const html = renderHtml(code);
      const htmlPath = `/tmp/_mermaid_render.html`;
      fs.writeFileSync(htmlPath, html);
      await page.setViewport({ width: 1400, height: 900, deviceScaleFactor: 2 });
      await page.goto('file://' + htmlPath, { waitUntil: 'networkidle2', timeout: 14000 });
      await page.waitForFunction(() => window.__done === true, { timeout: 14000 });
      const el = await page.$('#d');
      if (!el) { console.error('No element for', basename, i); continue; }
      await el.screenshot({ path: outPath, omitBackground: false });
      manifest[basename].push({ index: i, path: outPath, hash });
      console.log('Rendered', outPath);
    }
  }
  fs.writeFileSync(path.join(OUT_DIR, 'manifest.json'), JSON.stringify(manifest, null, 2));
  await browser.close();
  console.log('Manifest written to', path.join(OUT_DIR, 'manifest.json'));
})().catch(e => { console.error(e); process.exit(1); });
