// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { browser } from '../browser'

let page: Page
beforeEach(async () => {
  page = await browser.newPage()
})

describe('Dummy test', () => {
  it('works', async () => {
    await page.waitForSelector('body', { state: 'visible' })
  })
})
