// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import {
  CitizenCustomizations,
  CommonCustomizations,
  DeepPartial,
  EmployeeCustomizations,
  EmployeeMobileCustomizations
} from 'lib-customizations/types'

import config from './config'
import { setTestMode } from './generated/api-clients'

declare global {
  // eslint-disable-next-line no-var
  var evaka:
    | {
        captureScreenshots: (namePrefix: string) => Promise<void>
        saveTraces: (namePrefix: string) => Promise<void>
        promises: Promise<void>[]
      }
    | undefined
}

let browser: Browser

const DISABLE_JEST_TIMEOUT = 1_000_000_000 // 0 or Infinity unfortunately don't work
jest.setTimeout(config.playwright.headless ? 60_000 : DISABLE_JEST_TIMEOUT)

beforeAll(async () => {
  await setTestMode({ enabled: true })
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
  await setTestMode({ enabled: false })
})

const initScript = (options: EvakaBrowserContextOptions) => {
  const override = (key: keyof EvakaBrowserContextOptions) => {
    const value = options[key]
    return value
      ? `window.evaka.${key} = JSON.parse('${JSON.stringify(value)}')`
      : ''
  }
  const { mockedTime } = options

  return `
window.evaka = window.evaka ?? {}
window.evaka.automatedTest = true
${
  mockedTime
    ? `window.evaka.mockedTime = new Date('${mockedTime.toString()}')`
    : ''
}
${override('citizenCustomizations')}
${override('commonCustomizations')}
${override('employeeCustomizations')}
${override('employeeMobileCustomizations')}
  `
}

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
    if (!page.isClosed()) {
      await page.screenshot({
        type: 'png',
        path: `screenshots/${namePrefix}.${ctxIndex}.${pageIndex}.png`,
        timeout: 30_000
      })
    }
    // check again in case the previous screenshot operation crashed/closed something
    if (!page.isClosed()) {
      await page.screenshot({
        type: 'png',
        path: `screenshots/${namePrefix}.${ctxIndex}.full.${pageIndex}.png`,
        fullPage: true,
        timeout: 30_000
      })
    }
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
        // eslint-disable-next-line no-console
        console.log(`page ${page.url()} console.${msg.type()}`, ...args)
      })()
  )
  page.on('pageerror', (err) => {
    // eslint-disable-next-line no-console
    console.log(`Page ${page.url()}`, err)
  })
  page.on('crash', () => {
    // eslint-disable-next-line no-console
    console.log(`Page ${page.url()} crashed`)
  })
}

export interface EvakaBrowserContextOptions {
  mockedTime?: HelsinkiDateTime
  citizenCustomizations?: DeepPartial<JsonOf<CitizenCustomizations>>
  employeeCustomizations?: DeepPartial<JsonOf<EmployeeCustomizations>>
  employeeMobileCustomizations?: DeepPartial<
    JsonOf<EmployeeMobileCustomizations>
  >
  commonCustomizations?: DeepPartial<JsonOf<CommonCustomizations>>
}

export async function newBrowserContext(
  options?: BrowserContextOptions & EvakaBrowserContextOptions
): Promise<BrowserContext> {
  const {
    mockedTime,
    citizenCustomizations,
    employeeCustomizations,
    employeeMobileCustomizations,
    commonCustomizations,
    ...otherOptions
  } = options ?? {}
  const ctx = await browser.newContext(otherOptions)
  await ctx.tracing.start({
    snapshots: true,
    screenshots: true,
    sources: true
  })
  ctx.on('page', configurePage)
  ctx.setDefaultTimeout(config.playwright.ci ? 30_000 : 5_000)
  await ctx.addInitScript({
    content: initScript({
      mockedTime,
      citizenCustomizations,
      employeeCustomizations,
      employeeMobileCustomizations,
      commonCustomizations
    })
  })
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

export const mobileViewport = { width: 360, height: 740 }
