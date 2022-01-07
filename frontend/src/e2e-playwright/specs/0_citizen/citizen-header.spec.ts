// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { enduserLogin } from 'e2e-playwright/utils/user'
import { resetDatabase } from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'

let page: Page
let header: CitizenHeader

beforeEach(async () => {
  await resetDatabase()
  await initializeAreaAndPersonData()

  page = await Page.open()
  await enduserLogin(page)
  header = new CitizenHeader(page)
})

describe('Citizen page', () => {
  test('UI language can be changed', async () => {
    await header.waitUntilLoggedIn()

    await header.selectLanguage('fi')
    await header.assertApplyingTabHasText('Hakeminen')

    await header.selectLanguage('sv')
    await header.assertApplyingTabHasText('Ansökningar')

    await header.selectLanguage('en')
    await header.assertApplyingTabHasText('Applications')
  })
})
