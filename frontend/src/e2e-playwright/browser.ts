// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import path from 'path'
import playwright, {
  Browser,
  BrowserContext,
  BrowserContextOptions,
  Download
} from 'playwright'
import config from 'e2e-test-common/config'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace NodeJS {
    interface Global {
      evaka?: {
        takeScreenshots: (namePrefix: string) => Promise<void>
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
  global.evaka = { takeScreenshots }
})

afterEach(async () => {
  for (const ctx of browser?.contexts() ?? []) {
    await ctx.close()
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

async function takeScreenshots(namePrefix: string): Promise<void> {
  if (!browser) return
  const pages = browser.contexts().flatMap((ctx, ctxIndex) =>
    ctx.pages().map((page, pageIndex) => ({
      ctxIndex,
      pageIndex,
      page
    }))
  )
  for (const { ctxIndex, pageIndex, page } of pages) {
    await page.screenshot({
      type: 'png',
      path: `screenshots/${namePrefix}.${ctxIndex}.${pageIndex}.png`
    })
  }
}

export async function newBrowserContext(
  options?: BrowserContextOptions
): Promise<BrowserContext> {
  const ctx = await browser.newContext(options)
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
