// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import playwright, {
  Browser,
  BrowserContext,
  BrowserContextOptions,
  Download,
  Page
} from 'playwright'
import config from './config'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace NodeJS {
    interface Global {
      evaka?: {
        captureScreenshots: (namePrefix: string) => Promise<void>
        saveTraces: (namePrefix: string) => Promise<void>
        promises: Promise<void>[]
      }
    }
  }
}

let browser: Browser

const DISABLE_JEST_TIMEOUT = 1_000_000_000 // 0 or Infinity unfortunately don't work

function configureJestTimeout() {
  jest.setTimeout(config.playwright.ci ? 60_000 : DISABLE_JEST_TIMEOUT)
}

beforeEach((done) => {
  configureJestTimeout()
  done()
})

beforeAll(async () => {
  configureJestTimeout()
  browser = await playwright[config.playwright.browser].launch({
    headless: config.playwright.headless,
    tracesDir: '/tmp/playwright-traces'
  })
  global.evaka = {
    captureScreenshots,
    saveTraces,
    promises: []
  }
})

afterEach(async () => {
  if (global.evaka) {
    await Promise.all(global.evaka.promises)
    global.evaka.promises = []
  }
  for (const ctx of browser?.contexts() ?? []) {
    await ctx.close()
  }
})

afterAll(async () => {
  await browser?.close()
  delete global.evaka
})

const initScript = (mockedTime?: Date) => `
window.evakaAutomatedTest = true
${mockedTime ? `window.evakaMockedTime = '${mockedTime.toISOString()}'` : ''}
`

async function forEachContext(
  fn: (ctxInfo: { ctx: BrowserContext; ctxIndex: number }) => Promise<void>
): Promise<void> {
  if (!browser) return
  const contexts = browser.contexts().map((ctx, i) => [ctx, i] as const)
  for (const [ctx, ctxIndex] of contexts) {
    await fn({ ctx, ctxIndex })
  }
}

async function forEachPage(
  fn: (pageInfo: {
    ctx: BrowserContext
    ctxIndex: number
    page: Page
    pageIndex: number
  }) => Promise<void>
): Promise<void> {
  await forEachContext(async ({ ctx, ctxIndex }) => {
    const pages = ctx.pages().map((page, i) => [page, i] as const)
    for (const [page, pageIndex] of pages) {
      await fn({ ctx, ctxIndex, page, pageIndex })
    }
  })
}

async function captureScreenshots(namePrefix: string): Promise<void> {
  await forEachPage(async ({ ctxIndex, pageIndex, page }) => {
    await page.screenshot({
      type: 'png',
      path: `screenshots/${namePrefix}.${ctxIndex}.${pageIndex}.png`
    })
    await page.screenshot({
      type: 'png',
      path: `screenshots/${namePrefix}.${ctxIndex}.full.${pageIndex}.png`,
      fullPage: true
    })
  })
}

async function saveTraces(namePrefix: string): Promise<void> {
  await forEachContext(async ({ ctxIndex, ctx }) => {
    await ctx.tracing.stop({ path: `traces/${namePrefix}.${ctxIndex}.zip` })
  })
}

function configurePage(page: Page) {
  page.on(
    'console',
    (msg) =>
      void (async () => {
        const args: unknown[] = []
        for (const arg of msg.args()) {
          const value = (await arg.jsonValue()) as unknown
          args.push(value)
        }
        if (args.length === 0) {
          args.push(msg.text())
        }
        // Unnecessary spam
        if (
          typeof args[0] === 'string' &&
          args[0].includes('Download the React DevTools')
        ) {
          return
        }
        console.log(`page ${page.url()} console.${msg.type()}`, ...args)
      })()
  )
  page.on('pageerror', (err) => {
    console.log(`Page ${page.url()}`, err)
  })
}

export async function newBrowserContext(
  options?: BrowserContextOptions & { mockedTime?: Date }
): Promise<BrowserContext> {
  const ctx = await browser.newContext(options)
  await ctx.tracing.start({
    snapshots: true,
    screenshots: true,
    sources: true
  })
  ctx.on('page', configurePage)
  ctx.setDefaultTimeout(config.playwright.ci ? 30_000 : 5_000)
  await ctx.addInitScript({ content: initScript(options?.mockedTime) })
  return ctx
}

export async function captureTextualDownload(
  download: Download
): Promise<string> {
  const filePath = await download.path()
  if (!filePath) throw new Error('Download failed')
  return new Promise<string>((resolve, reject) =>
    fs.readFile(filePath, 'utf-8', (err, data) =>
      err ? reject(err) : resolve(data)
    )
  )
}
