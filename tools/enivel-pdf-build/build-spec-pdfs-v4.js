const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const SPEC_DIR = '/Volumes/Evaka/enivel/specification';
const LIITTEET_DIR = '/Volumes/Evaka/enivel/liiteet';
const OUT_DIR = '/Volumes/Evaka/enivel/pdf';
const DATE = '2026-05-28';

const CHAPTERS = [
  { slug: '01-johdanto', file: '01-johdanto.md', title: '1. Johdanto', root: SPEC_DIR },
  { slug: '02-jarjestelmaarkitehtuuri', file: '02-jarjestelmaarkitehtuuri.md', title: '2. Järjestelmäarkkitehtuuri', root: SPEC_DIR },
  { slug: '03-tietomalli', file: '03-tietomalli.md', title: '3. Tietomalli', root: SPEC_DIR },
  { slug: '04-kayttotapaukset', file: '04-kayttotapaukset.md', title: '4. Käyttötapaukset ja vaatimukset', root: SPEC_DIR },
  { slug: '05-integraatiovaatimukset', file: '05-integraatiovaatimukset.md', title: '5. Integraatiovaatimukset', root: SPEC_DIR },
  { slug: '06-ei-funktionaaliset-vaatimukset', file: '06-ei-funktionaaliset-vaatimukset.md', title: '6. Ei-funktionaaliset vaatimukset', root: SPEC_DIR },
  { slug: '07-toteutus-nakokulmat', file: '07-toteutus-nakokulmat.md', title: '7. Toteutuksen näkökulmat', root: SPEC_DIR },
];

const LIITTEET = [
  { slug: 'A-tekniset-yksityiskohdat', file: 'A-tekniset-yksityiskohdat.md', title: 'Liite A — Toteutuksen tekniset yksityiskohdat', root: LIITTEET_DIR },
  { slug: 'B-kayttoliittymaluonnokset', file: 'B-kayttoliittymaluonnokset.md', title: 'Liite B — Käyttöliittymäluonnokset ja muut artefaktit', root: LIITTEET_DIR },
];

const CSS = `
  @page { size: A4; margin: 20mm 18mm; }
  body {
    font-family: -apple-system, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
    line-height: 1.55;
    color: #1f1f1f;
    background: #ffffff;
    font-size: 10pt;
    margin: 0;
    padding: 0;
  }
  .doc-meta { color: #6e6e6e; font-size: 9.5pt; margin-bottom: 14pt; }
  h1 {
    color: #2c5fa1; border-bottom: 2.5pt solid #2c5fa1; padding-bottom: 6pt;
    font-size: 26pt; margin: 0 0 16pt 0; font-weight: 700; letter-spacing: -0.01em;
    page-break-after: avoid;
  }
  h2 {
    color: #2c5fa1; font-size: 17pt; margin-top: 20pt; margin-bottom: 8pt;
    border-bottom: 1pt solid #2c5fa1; padding-bottom: 3pt; font-weight: 700;
    page-break-after: avoid;
  }
  h3 { color: #1f1f1f; font-size: 13pt; margin-top: 16pt; margin-bottom: 6pt; font-weight: 700; page-break-after: avoid; }
  h4 { color: #1f1f1f; font-size: 11pt; margin-top: 12pt; margin-bottom: 4pt; font-weight: 700; page-break-after: avoid; }
  p { margin: 6pt 0; }
  code { background: #f1ebe3; color: #b8421a; padding: 1pt 4pt; border-radius: 2pt;
    font-family: "SF Mono", Menlo, Consolas, monospace; font-size: 8.8pt; }
  pre { background: #f5f5f5; padding: 9pt 11pt; border-radius: 3pt; overflow-x: auto;
    font-size: 8.6pt; line-height: 1.45; page-break-inside: avoid; margin: 8pt 0; }
  pre code { background: transparent; color: #1f1f1f; padding: 0; font-size: inherit; }
  table { border-collapse: collapse; margin: 10pt 0; width: 100%; page-break-inside: avoid; font-size: 9pt; }
  th, td { border: 0.5pt solid #b8b8b8; padding: 5pt 7pt; text-align: left; vertical-align: top; }
  th { background: #ececec; color: #1f1f1f; font-weight: 700; }
  blockquote { background: #e8eef5; border-left: 3pt solid #2c5fa1; padding: 8pt 14pt;
    margin: 10pt 0; color: #2a2a2a; font-style: italic; page-break-inside: avoid; }
  blockquote p { margin: 4pt 0; }
  blockquote strong { color: #2c5fa1; font-style: normal; }
  blockquote em { font-style: italic; }
  .mermaid { text-align: center; margin: 16pt 0; page-break-inside: avoid; }
  .mermaid svg { max-width: 100%; height: auto; }
  ul, ol { margin: 6pt 0; padding-left: 22pt; }
  li { margin: 3pt 0; }
  hr.pagebreak { page-break-before: always; visibility: hidden; height: 0; border: none; margin: 0; }
  hr:not(.pagebreak) { border: none; border-top: 0.5pt solid #b8b8b8; margin: 14pt 0; }
  a { color: #2c5fa1; text-decoration: none; }
  strong { color: #1f1f1f; font-weight: 700; }
  em { font-style: italic; }
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
<div class="doc-meta">eNivel-määrittely · Reaktor · ${DATE}</div>
<div id="content"></div>
<script src="https://cdn.jsdelivr.net/npm/marked@12/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<script>
  const md = ${JSON.stringify(md)};
  const renderer = new marked.Renderer();
  const origCode = renderer.code.bind(renderer);
  renderer.code = function(code, lang) {
    if (lang === 'mermaid') return '<div class="mermaid">' + code + '</div>';
    return origCode(code, lang);
  };
  const expanded = md.replace(/<!-- PAGEBREAK -->/g, '\\n\\n<hr class="pagebreak" />\\n\\n');
  marked.setOptions({ gfm: true, breaks: false });
  document.getElementById('content').innerHTML = marked.parse(expanded, { renderer });
  mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' });
  mermaid.run({ querySelector: '.mermaid' }).then(() => { window.__renderingDone = true; })
    .catch(e => { console.error('mermaid error:', e); window.__renderingDone = true; });
</script>
</body>
</html>`;
}

const headerTemplate = left => `<div style="font-size:8pt; width:100%; padding:0 18mm; color:#6e6e6e; font-family:-apple-system,sans-serif; display:flex; justify-content:space-between;">
    <span>${left}</span><span>eNivel</span>
  </div>`;

const FOOTER_TEMPLATE = `<div style="font-size:8pt; width:100%; text-align:center; color:#6e6e6e; font-family:-apple-system,sans-serif;">
  <span class="pageNumber"></span> / <span class="totalPages"></span>
</div>`;

async function buildOne(page, md, outPath, headerLeft) {
  const html = renderPage(md, headerLeft);
  const htmlPath = `/tmp/enivel-pdf-build/_render.html`;
  fs.writeFileSync(htmlPath, html);
  await page.goto('file://' + htmlPath, { waitUntil: 'networkidle2', timeout: 14000 });
  await page.waitForFunction(() => window.__renderingDone === true, { timeout: 14000 });
  await page.pdf({
    path: outPath,
    format: 'A4',
    printBackground: true,
    margin: { top: '20mm', bottom: '18mm', left: '18mm', right: '18mm' },
    displayHeaderFooter: true,
    headerTemplate: headerTemplate(headerLeft),
    footerTemplate: FOOTER_TEMPLATE,
  });
}

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  try {
    const page = await browser.newPage();
    page.on('console', m => { if (m.type() === 'error') console.error('PAGE:', m.text()); });

    // 1. Pää-spec yhdistetty
    const indexMd = fs.readFileSync(path.join(SPEC_DIR, 'enivel-specification.md'), 'utf8');
    const bodyMd = CHAPTERS.map(c => `\n\n<!-- PAGEBREAK -->\n\n` + fs.readFileSync(path.join(c.root, c.file), 'utf8')).join('');
    await buildOne(page, indexMd + bodyMd,
      path.join(OUT_DIR, `enivel-jarjestelmamaarittely-${DATE}.pdf`),
      'eNivel-määrittely');
    console.log('Wrote combined spec PDF');

    // 2. Pää-spec lukukohtaiset
    for (const c of CHAPTERS) {
      const md = fs.readFileSync(path.join(c.root, c.file), 'utf8');
      await buildOne(page, md, path.join(OUT_DIR, `${c.slug}.pdf`), c.title);
      console.log('Wrote', c.slug);
    }

    // 3. Liitteet yhdistetty
    const liitteetIndex = fs.readFileSync(path.join(LIITTEET_DIR, 'enivel-liiteet.md'), 'utf8');
    const liitteetBody = LIITTEET.map(c => `\n\n<!-- PAGEBREAK -->\n\n` + fs.readFileSync(path.join(c.root, c.file), 'utf8')).join('');
    await buildOne(page, liitteetIndex + liitteetBody,
      path.join(OUT_DIR, `enivel-liiteet-${DATE}.pdf`),
      'eNivel-liiteet');
    console.log('Wrote combined liiteet PDF');

    // 4. Liitteet erikseen
    for (const c of LIITTEET) {
      const md = fs.readFileSync(path.join(c.root, c.file), 'utf8');
      await buildOne(page, md, path.join(OUT_DIR, `${c.slug}.pdf`), c.title);
      console.log('Wrote', c.slug);
    }
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
