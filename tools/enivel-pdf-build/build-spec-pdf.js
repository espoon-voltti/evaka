const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const SPEC_DIR = '/Volumes/Evaka/enivel/specification';
const FILES = [
  '01-johdanto.md',
  '02-jarjestelmaarkitehtuuri.md',
  '03-tietomalli.md',
  '04-kayttotapaukset.md',
  '05-integraatiovaatimukset.md',
  '06-ei-funktionaaliset-vaatimukset.md',
  '07-toteutus-nakokulmat.md',
];

const indexMd = fs.readFileSync(path.join(SPEC_DIR, 'enivel-specification.md'), 'utf8');
const bodyMd = FILES.map(f => fs.readFileSync(path.join(SPEC_DIR, f), 'utf8')).join('\n\n<!-- PAGEBREAK -->\n\n');
const md = indexMd + '\n\n<!-- PAGEBREAK -->\n\n' + bodyMd;

const html = `<!DOCTYPE html>
<html lang="fi">
<head>
  <meta charset="utf-8">
  <title>eNivel — Järjestelmämäärittely</title>
  <style>
    @page { size: A4; margin: 18mm 16mm; }
    body {
      font-family: -apple-system, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
      line-height: 1.45;
      color: #1f1f1f;
      font-size: 10pt;
    }
    h1 {
      page-break-before: always;
      border-bottom: 2px solid #2b2b2b;
      padding-bottom: 8px;
      font-size: 22pt;
      margin-top: 0;
      margin-bottom: 12pt;
    }
    h1:first-of-type { page-break-before: avoid; }
    h2 { font-size: 16pt; margin-top: 18pt; border-bottom: 1px solid #ccc; padding-bottom: 4px; }
    h3 { font-size: 13pt; margin-top: 14pt; }
    h4 { font-size: 11pt; margin-top: 12pt; }
    p { margin: 6pt 0; }
    code {
      background: #f0f0f0;
      padding: 1px 4px;
      border-radius: 3px;
      font-family: "SF Mono", Menlo, Consolas, monospace;
      font-size: 9pt;
    }
    pre {
      background: #f5f5f5;
      padding: 8pt 10pt;
      border-radius: 4px;
      overflow-x: auto;
      font-size: 8.5pt;
      line-height: 1.4;
      page-break-inside: avoid;
    }
    pre code { background: transparent; padding: 0; font-size: inherit; }
    table {
      border-collapse: collapse;
      margin: 8pt 0;
      width: 100%;
      page-break-inside: avoid;
      font-size: 9pt;
    }
    th, td {
      border: 1px solid #bbb;
      padding: 4pt 6pt;
      text-align: left;
      vertical-align: top;
    }
    th { background: #ececec; font-weight: 600; }
    blockquote {
      border-left: 3px solid #888;
      padding: 4pt 12pt;
      margin: 8pt 0;
      color: #444;
      background: #fafafa;
      page-break-inside: avoid;
    }
    blockquote p { margin: 4pt 0; }
    .mermaid {
      text-align: center;
      margin: 16pt 0;
      page-break-inside: avoid;
    }
    .mermaid svg { max-width: 100%; height: auto; }
    ul, ol { margin: 6pt 0; padding-left: 22pt; }
    li { margin: 3pt 0; }
    hr.pagebreak { page-break-before: always; visibility: hidden; height: 0; border: none; }
    a { color: #1a5fb4; text-decoration: none; }
    .cover {
      page-break-after: always;
      text-align: center;
      padding-top: 30vh;
    }
    .cover h1 {
      border: none;
      font-size: 28pt;
      page-break-before: avoid;
    }
    .cover .meta {
      margin-top: 40pt;
      color: #555;
      font-size: 11pt;
    }
  </style>
</head>
<body>
<div id="content"></div>
<script src="https://cdn.jsdelivr.net/npm/marked@12/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<script>
  const md = ${JSON.stringify(md)};

  const renderer = new marked.Renderer();
  const origCode = renderer.code.bind(renderer);
  renderer.code = function(code, lang) {
    if (lang === 'mermaid') {
      return '<div class="mermaid">' + code + '</div>';
    }
    return origCode(code, lang);
  };

  // PAGEBREAK -> hr.pagebreak
  const expanded = md.replace(/<!-- PAGEBREAK -->/g, '\\n\\n<hr class="pagebreak" />\\n\\n');

  marked.setOptions({ gfm: true, breaks: false });
  document.getElementById('content').innerHTML = marked.parse(expanded, { renderer });

  mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
  mermaid.run({ querySelector: '.mermaid' }).then(() => {
    window.__renderingDone = true;
  }).catch(e => {
    console.error('mermaid error:', e);
    window.__renderingDone = true;
  });
</script>
</body>
</html>`;

const HTML_OUT = '/tmp/enivel-pdf-build/spec-rendered.html';
const PDF_OUT = '/Volumes/Evaka/enivel/pdf/enivel-jarjestelmamaarittely-2026-05-28.pdf';

fs.writeFileSync(HTML_OUT, html);

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  try {
    const page = await browser.newPage();
    page.on('console', m => { if (m.type() === 'error') console.error('PAGE:', m.text()); });
    await page.goto('file://' + HTML_OUT, { waitUntil: 'networkidle2', timeout: 14000 });
    await page.waitForFunction(() => window.__renderingDone === true, { timeout: 14000 });
    await page.pdf({
      path: PDF_OUT,
      format: 'A4',
      printBackground: true,
      margin: { top: '18mm', bottom: '18mm', left: '16mm', right: '16mm' },
      displayHeaderFooter: true,
      headerTemplate: '<div></div>',
      footerTemplate: '<div style="font-size:8pt; width:100%; text-align:center; color:#888;">eNivel — Järjestelmämäärittely v0.2 · 28.5.2026 · <span class="pageNumber"></span> / <span class="totalPages"></span></div>',
    });
    console.log('Wrote', PDF_OUT);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
