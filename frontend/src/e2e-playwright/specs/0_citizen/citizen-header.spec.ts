// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import { resetDatabase } from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import { enduserLogin } from 'e2e-playwright/utils/user'
import { newBrowserContext } from '../../browser'
import { waitUntilEqual } from '../../utils'
import CitizenHeader from '../../pages/citizen/citizen-header'

let page: Page
let header: CitizenHeader

beforeEach(async () => {
  await resetDatabase()
  await initializeAreaAndPersonData()

  page = await (await newBrowserContext()).newPage()
  await page.goto(config.enduserUrl)
  header = new CitizenHeader(page)
})
afterEach(async () => {
  await page.close()
})

describe('Citizen page', () => {
  test('UI language can be changed', async () => {
    await enduserLogin(page)
    await header.selectLanguage('fi')
    await waitUntilEqual(
      async () => (await header.applicationsTab.innerText).toLowerCase(),
      'hakemukset'
    )
    await header.selectLanguage('sv')
    await waitUntilEqual(
      async () => (await header.applicationsTab.innerText).toLowerCase(),
      'ansökningar'
    )
    await header.selectLanguage('en')
    await waitUntilEqual(
      async () => (await header.applicationsTab.innerText).toLowerCase(),
      'applications'
    )
  })
})
