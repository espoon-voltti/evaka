const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const SPEC_DIR = '/Volumes/Evaka/enivel/specification';
const OUT_DIR = '/Volumes/Evaka/enivel/pdf';
const DATE = '2026-05-28';
const VERSION = 'v0.2';

const CHAPTERS = [
  { slug: '01-johdanto', file: '01-johdanto.md', title: '1. Johdanto' },
  { slug: '02-jarjestelmaarkitehtuuri', file: '02-jarjestelmaarkitehtuuri.md', title: '2. Järjestelmäarkkitehtuuri' },
  { slug: '03-tietomalli', file: '03-tietomalli.md', title: '3. Tietomalli' },
  { slug: '04-kayttotapaukset', file: '04-kayttotapaukset.md', title: '4. Käyttötapaukset ja vaatimukset' },
  { slug: '05-integraatiovaatimukset', file: '05-integraatiovaatimukset.md', title: '5. Integraatiovaatimukset' },
  { slug: '06-ei-funktionaaliset-vaatimukset', file: '06-ei-funktionaaliset-vaatimukset.md', title: '6. Ei-funktionaaliset vaatimukset' },
  { slug: '07-toteutus-nakokulmat', file: '07-toteutus-nakokulmat.md', title: '7. Toteutuksen näkökulmat' },
];

const CSS = `
  :root {
    --bg: #f6f4ee;
    --fg: #1f1f1f;
    --muted: #5d5d5d;
    --card: #ffffff;
    --border: #2b2b2b;
    --accent: #e6553f;
    --accent-soft: #ffe2db;
    --highlight: #fff4a3;
    --highlight-edge: #c9a200;
    --orange: #d97e00;
  }
  @page {
    size: A4;
    margin: 18mm 16mm;
  }
  body {
    font-family: -apple-system, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
    line-height: 1.5;
    color: var(--fg);
    background: var(--bg);
    font-size: 10pt;
    margin: 0;
    padding: 0;
  }
  .cover {
    page-break-after: always;
    padding: 30vh 0 0 0;
    text-align: center;
    background: var(--bg);
  }
  .cover .badge {
    display: inline-block;
    background: var(--accent);
    color: white;
    padding: 4pt 14pt;
    border-radius: 4pt;
    font-size: 10pt;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    margin-bottom: 20pt;
  }
  .cover h1 {
    color: var(--accent);
    border: none;
    font-size: 36pt;
    margin: 0 0 10pt 0;
    letter-spacing: -0.02em;
    page-break-before: avoid;
  }
  .cover .subtitle {
    font-size: 18pt;
    color: var(--fg);
    margin-bottom: 30pt;
  }
  .cover .meta {
    color: var(--muted);
    font-size: 11pt;
  }
  .cover .meta strong { color: var(--fg); }
  h1 {
    color: var(--accent);
    border-bottom: 3px solid var(--accent);
    padding-bottom: 6pt;
    font-size: 22pt;
    margin-top: 0;
    margin-bottom: 14pt;
    page-break-before: always;
    page-break-after: avoid;
  }
  h1:first-of-type { page-break-before: avoid; }
  h2 {
    color: var(--fg);
    font-size: 15pt;
    margin-top: 20pt;
    border-bottom: 1px solid var(--highlight-edge);
    padding-bottom: 3pt;
    page-break-after: avoid;
  }
  h3 {
    color: var(--orange);
    font-size: 12pt;
    margin-top: 15pt;
    page-break-after: avoid;
  }
  h4 {
    color: var(--fg);
    font-size: 10.5pt;
    margin-top: 12pt;
    page-break-after: avoid;
  }
  p { margin: 6pt 0; }
  code {
    background: #f0e8e0;
    color: #b8421a;
    padding: 1pt 5pt;
    border-radius: 3pt;
    font-family: "SF Mono", Menlo, Consolas, monospace;
    font-size: 8.8pt;
  }
  pre {
    background: #f5efe6;
    border-left: 3px solid var(--orange);
    padding: 8pt 12pt;
    border-radius: 0 4pt 4pt 0;
    overflow-x: auto;
    font-size: 8.5pt;
    line-height: 1.45;
    page-break-inside: avoid;
  }
  pre code {
    background: transparent;
    color: var(--fg);
    padding: 0;
    font-size: inherit;
  }
  table {
    border-collapse: collapse;
    margin: 10pt 0;
    width: 100%;
    page-break-inside: avoid;
    font-size: 9pt;
    background: var(--card);
    box-shadow: 2pt 2pt 0 0 rgba(0,0,0,0.05);
  }
  th, td {
    border: 1px solid #d4cebd;
    padding: 5pt 8pt;
    text-align: left;
    vertical-align: top;
  }
  th {
    background: var(--accent-soft);
    color: var(--accent);
    font-weight: 700;
  }
  tr:nth-child(even) td { background: #faf7f0; }
  blockquote {
    background: var(--highlight);
    border-left: 4px solid var(--highlight-edge);
    padding: 8pt 14pt;
    margin: 10pt 0;
    color: var(--fg);
    border-radius: 0 4pt 4pt 0;
    page-break-inside: avoid;
  }
  blockquote p { margin: 4pt 0; }
  blockquote strong { color: var(--accent); }
  .mermaid {
    text-align: center;
    margin: 18pt 0;
    page-break-inside: avoid;
    background: var(--card);
    padding: 12pt;
    border-radius: 6pt;
    border: 1px solid #d4cebd;
  }
  .mermaid svg { max-width: 100%; height: auto; }
  ul, ol { margin: 6pt 0; padding-left: 22pt; }
  li { margin: 3pt 0; }
  li::marker { color: var(--accent); }
  hr.pagebreak { page-break-before: always; visibility: hidden; height: 0; border: none; margin: 0; }
  hr:not(.pagebreak) {
    border: none;
    border-top: 1px dashed var(--highlight-edge);
    margin: 16pt 0;
  }
  a { color: var(--accent); text-decoration: none; border-bottom: 1px dotted var(--accent); }
  strong { color: var(--fg); }
  em { color: var(--muted); font-style: italic; }
`;

function renderPage(md, title) {
  return `<!DOCTYPE html>
<html lang="fi">
<head>
  <meta charset="utf-8">
  <title>${title}</title>
  <style>${CSS}</style>
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
  const expanded = md.replace(/<!-- PAGEBREAK -->/g, '\\n\\n<hr class="pagebreak" />\\n\\n');
  marked.setOptions({ gfm: true, breaks: false });
  document.getElementById('content').innerHTML = marked.parse(expanded, { renderer });
  mermaid.initialize({
    startOnLoad: false,
    theme: 'base',
    themeVariables: {
      primaryColor: '#ffe2db',
      primaryTextColor: '#1f1f1f',
      primaryBorderColor: '#e6553f',
      lineColor: '#5d5d5d',
      secondaryColor: '#fff4a3',
      tertiaryColor: '#f6f4ee',
      fontFamily: '-apple-system, sans-serif',
    },
    securityLevel: 'loose',
  });
  mermaid.run({ querySelector: '.mermaid' }).then(() => {
    window.__renderingDone = true;
  }).catch(e => {
    console.error('mermaid error:', e);
    window.__renderingDone = true;
  });
</script>
</body>
</html>`;
}

const COVER = `<div class="cover">
  <div class="badge">Reaktor × Turku × Oulu</div>
  <h1>eNivel</h1>
  <div class="subtitle">Järjestelmämäärittely</div>
  <div class="meta">
    Versio <strong>${VERSION}</strong> · ${DATE}<br>
    Esi- ja perusopetuksen välisen tiedonsiirron tekninen määrittely<br>
    OKM:n valtionavustus <code>va-okm-2025-21-42</code>
  </div>
</div>

<!-- PAGEBREAK -->

`;

const FOOTER_TEMPLATE = `<div style="font-size:8pt; width:100%; text-align:center; color:#5d5d5d; font-family:-apple-system,sans-serif;">
  eNivel — Järjestelmämäärittely ${VERSION} · ${DATE} ·
  <span class="pageNumber"></span> / <span class="totalPages"></span>
</div>`;

const HEADER_TEMPLATE = `<div></div>`;

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  try {
    const page = await browser.newPage();
    page.on('console', m => { if (m.type() === 'error') console.error('PAGE:', m.text()); });

    // 1. Yhdistetty
    const indexMd = fs.readFileSync(path.join(SPEC_DIR, 'enivel-specification.md'), 'utf8');
    const bodyMd = CHAPTERS.map(c => fs.readFileSync(path.join(SPEC_DIR, c.file), 'utf8'))
      .join('\n\n<!-- PAGEBREAK -->\n\n');
    const combined = COVER + indexMd + '\n\n<!-- PAGEBREAK -->\n\n' + bodyMd;
    const combinedHtml = renderPage(combined, 'eNivel — Järjestelmämäärittely');
    const combinedHtmlPath = '/tmp/enivel-pdf-build/combined.html';
    fs.writeFileSync(combinedHtmlPath, combinedHtml);
    await page.goto('file://' + combinedHtmlPath, { waitUntil: 'networkidle2', timeout: 14000 });
    await page.waitForFunction(() => window.__renderingDone === true, { timeout: 14000 });
    await page.pdf({
      path: path.join(OUT_DIR, `enivel-jarjestelmamaarittely-${DATE}.pdf`),
      format: 'A4',
      printBackground: true,
      margin: { top: '18mm', bottom: '18mm', left: '16mm', right: '16mm' },
      displayHeaderFooter: true,
      headerTemplate: HEADER_TEMPLATE,
      footerTemplate: FOOTER_TEMPLATE,
    });
    console.log('Wrote combined PDF');

    // 2. Lukukohtaiset
    for (const c of CHAPTERS) {
      const md = fs.readFileSync(path.join(SPEC_DIR, c.file), 'utf8');
      const html = renderPage(md, c.title);
      const htmlPath = `/tmp/enivel-pdf-build/${c.slug}.html`;
      fs.writeFileSync(htmlPath, html);
      await page.goto('file://' + htmlPath, { waitUntil: 'networkidle2', timeout: 14000 });
      await page.waitForFunction(() => window.__renderingDone === true, { timeout: 14000 });
      await page.pdf({
        path: path.join(OUT_DIR, `${c.slug}-${DATE}.pdf`),
        format: 'A4',
        printBackground: true,
        margin: { top: '18mm', bottom: '18mm', left: '16mm', right: '16mm' },
        displayHeaderFooter: true,
        headerTemplate: HEADER_TEMPLATE,
        footerTemplate: FOOTER_TEMPLATE,
      });
      console.log('Wrote', c.slug);
    }
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
