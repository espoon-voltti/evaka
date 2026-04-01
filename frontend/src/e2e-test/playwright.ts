// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable react-hooks/rules-of-hooks */

import {
  expect as baseExpect,
  test as base,
  type BrowserContext,
  type BrowserContextOptions,
  type Locator
} from '@playwright/test'

import { initScript, type EvakaBrowserContextOptions } from './browser'
import config from './config'
import { setTestMode } from './generated/api-clients'
import { Element, ElementCollection, Page } from './utils/page'

export type NewEvakaPage = (
  options?: BrowserContextOptions & EvakaBrowserContextOptions
) => Promise<Page>

type EvakaFixtures = {
  testMode: void
  evakaOptions: EvakaBrowserContextOptions
  evaka: Page
  newEvakaPage: NewEvakaPage
}

export const test = base.extend<EvakaFixtures>({
  evakaOptions: [{}, { option: true }],

  context: async ({ context, evakaOptions }, use) => {
    await context.addInitScript({ content: initScript(evakaOptions) })
    await use(context)
  },

  page: async ({ page }, use) => {
    page.setDefaultTimeout(config.playwright.ci ? 30_000 : 5_000)
    await use(page)
  },

  evaka: async ({ page }, use) => {
    await use(new Page(page))
  },

  // Use the page fixture so that it's created
  newEvakaPage: async ({ page: _, browser, evakaOptions }, use) => {
    const contexts: BrowserContext[] = []

    const createPage = async (
      options?: BrowserContextOptions & EvakaBrowserContextOptions
    ): Promise<Page> => {
      const {
        mockedTime,
        citizenCustomizations,
        employeeCustomizations,
        employeeMobileCustomizations,
        commonCustomizations,
        ...browserOptions
      } = options ?? {}
      const mergedEvakaOptions = {
        ...evakaOptions,
        ...(mockedTime !== undefined && { mockedTime }),
        ...(citizenCustomizations !== undefined && { citizenCustomizations }),
        ...(employeeCustomizations !== undefined && { employeeCustomizations }),
        ...(employeeMobileCustomizations !== undefined && {
          employeeMobileCustomizations
        }),
        ...(commonCustomizations !== undefined && { commonCustomizations })
      }
      const context = await browser.newContext(browserOptions)
      contexts.push(context)
      const pwPage = await context.newPage()
      pwPage.setDefaultTimeout(config.playwright.ci ? 30_000 : 5_000)
      await context.addInitScript({ content: initScript(mergedEvakaOptions) })
      return new Page(pwPage)
    }

    await use(createPage)

    for (const context of contexts) {
      await context.close()
    }
  }
})

test.beforeAll(async () => {
  await setTestMode({ enabled: true })
})

test.afterAll(async () => {
  await setTestMode({ enabled: false })
})

function unwrapElement(value: unknown): unknown {
  if (value instanceof Element) return value.locator
  if (value instanceof ElementCollection) return value.locator
  return value
}

export const expect = new Proxy(baseExpect, {
  apply(target, thisArg, args: [unknown, ...unknown[]]) {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-return
    return Reflect.apply(target, thisArg, [
      unwrapElement(args[0]),
      ...args.slice(1)
    ])
  }
}) as {
  (value: Element): ReturnType<typeof baseExpect<Locator>>
  (value: ElementCollection): ReturnType<typeof baseExpect<Locator>>
} & typeof baseExpect
