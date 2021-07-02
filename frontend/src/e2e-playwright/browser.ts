// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import path from 'path'
import playwright, {
  Browser,
  BrowserContext,
  BrowserContextOptions,
  Download,
  Page
} from 'playwright'
import config from 'e2e-test-common/config'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace NodeJS {
    interface Global {
      evaka?: {
        captureScreenshots: (namePrefix: string) => Promise<void>
        saveVideos: (namePrefix: string) => Promise<void>
        deleteTemporaryVideos: () => Promise<void>
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
    headless: config.playwright.headless
  })
  global.evaka = {
    captureScreenshots,
    saveVideos,
    deleteTemporaryVideos,
    promises: []
  }
})

afterEach(async () => {
  for (const ctx of browser?.contexts() ?? []) {
    await ctx.close()
  }
  if (global.evaka) {
    await Promise.all(global.evaka.promises)
    global.evaka.promises = []
  }
})

afterAll(async () => {
  await browser?.close()
  delete global.evaka
})

const injected = fs.readFileSync(
  path.resolve(__dirname, '../e2e-test-common/injected.js'),
  'utf-8'
)

async function forEachPage(
  fn: (pageInfo: {
    ctxIndex: number
    pageIndex: number
    page: Page
  }) => Promise<void>
): Promise<void> {
  if (!browser) return
  const pages = browser.contexts().flatMap((ctx, ctxIndex) =>
    ctx.pages().map((page, pageIndex) => ({
      ctxIndex,
      pageIndex,
      page
    }))
  )
  for (const pageInfo of pages) {
    await fn(pageInfo)
  }
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

async function saveVideos(namePrefix: string): Promise<void> {
  await forEachPage(async ({ ctxIndex, pageIndex, page }) => {
    await page
      .video()
      ?.saveAs(`videos/${namePrefix}.${ctxIndex}.${pageIndex}.webm`)
  })
}

async function deleteTemporaryVideos(): Promise<void> {
  await forEachPage(async ({ page }) => {
    await page.video()?.delete()
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
  options?: BrowserContextOptions
): Promise<BrowserContext> {
  const recordVideoOptions = config.playwright.ci
    ? { recordVideo: { dir: '/tmp/playwright_videos' } }
    : undefined
  const ctx = await browser.newContext({ ...recordVideoOptions, ...options })
  ctx.on('page', configurePage)
  ctx.setDefaultTimeout(config.playwright.ci ? 30_000 : 5_000)
  await ctx.addInitScript({ content: injected })
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
