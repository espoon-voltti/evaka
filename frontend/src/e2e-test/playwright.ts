// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable react-hooks/rules-of-hooks */

import {
  test as base,
  type BrowserContext,
  type BrowserContextOptions
} from '@playwright/test'

import { initScript, type EvakaBrowserContextOptions } from './browser'
import config from './config'
import { setTestMode } from './generated/api-clients'
import { Page } from './utils/page'

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
  // Auto fixture that ensures test mode is enabled before any test code runs
  testMode: [
    // eslint-disable-next-line no-empty-pattern
    async ({}, use) => {
      await setTestMode({ enabled: true })
      await use()
      await setTestMode({ enabled: false })
    },
    { auto: true }
  ],

  evakaOptions: [{}, { option: true }],

  page: async ({ page, evakaOptions }, use) => {
    page.setDefaultTimeout(config.playwright.ci ? 30_000 : 5_000)
    await page.addInitScript({ content: initScript(evakaOptions) })
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
      await pwPage.addInitScript({ content: initScript(mergedEvakaOptions) })
      return new Page(pwPage)
    }

    await use(createPage)

    for (const context of contexts) {
      await context.close()
    }
  }
})

export { expect } from '@playwright/test'
