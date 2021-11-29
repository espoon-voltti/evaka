// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
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
  await enduserLogin(page)
  header = new CitizenHeader(page)
})

describe('Citizen page', () => {
  test('UI language can be changed', async () => {
    await header.waitUntilLoggedIn()

    await header.selectLanguage('fi')
    await waitUntilEqual(
      async () => (await header.applyingTab.innerText()).toLowerCase(),
      'hakeminen'
    )
    await header.selectLanguage('sv')
    await waitUntilEqual(
      async () => (await header.applyingTab.innerText()).toLowerCase(),
      'ansöker'
    )
    await header.selectLanguage('en')
    await waitUntilEqual(
      async () => (await header.applyingTab.innerText()).toLowerCase(),
      'applying'
    )
  })
})
