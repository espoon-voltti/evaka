// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { newBrowserContext } from '../browser'

let page: Page
beforeEach(async () => {
  page = await (await newBrowserContext()).newPage()
  await page.goto('about:blank')
})

describe('Dummy test', () => {
  it('works', async () => {
    await page.waitForSelector('body', { state: 'visible' })
    const isAutomatedTest = await page.evaluate(
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-explicit-any
      () => (window as any).evakaAutomatedTest as boolean | undefined
    )
    expect(isAutomatedTest).toBeTruthy()
  })
})
