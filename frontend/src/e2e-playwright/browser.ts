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

let browser: Browser

const DISABLE_JEST_TIMEOUT = 1_000_000_000 // 0 or Infinity unfortunately don't work

beforeAll(async () => {
  jest.setTimeout(config.playwright.ci ? 60_000 : DISABLE_JEST_TIMEOUT)
  browser = await playwright[config.playwright.browser].launch({
    headless: config.playwright.headless
  })
})

afterEach(async () => {
  for (const ctx of browser?.contexts() ?? []) {
    await ctx.close()
  }
})

afterAll(async () => {
  await browser?.close()
})

const injected = fs.readFileSync(
  path.resolve(__dirname, '../e2e-test-common/injected.js'),
  'utf-8'
)

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
