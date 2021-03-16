// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import path from 'path'
import {
  Browser,
  BrowserContext,
  BrowserContextOptions,
  chromium
} from 'playwright'

function envBoolean(name: string): boolean | undefined {
  switch (process.env[name]) {
    case 'true':
      return true
    case 'false':
      return false
    default:
      return undefined
  }
}

let browser: Browser

beforeAll(async () => {
  browser = await chromium.launch({
    headless: envBoolean('HEADLESS')
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
  await ctx.addInitScript({ content: injected })
  return ctx
}
