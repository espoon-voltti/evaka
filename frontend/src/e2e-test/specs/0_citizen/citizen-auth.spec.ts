// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { testAdult } from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

describe('Citizen authentication', () => {
  let page: Page
  const credentials = {
    username: 'test@example.com',
    password: 'TestPassword456!'
  }

  beforeEach(async () => {
    await resetServiceState()
    await testAdult.saveAdult({
      updateMockVtjWithDependants: []
    })
    await upsertWeakCredentials({
      id: testAdult.id,
      body: credentials
    })

    page = await Page.open()
  })

  const initConfigurations = [
    [
      'direct login',
      async (page: Page) => enduserLogin(page, testAdult)
    ] as const,
    [
      'weak login',
      async (page: Page) => enduserLoginWeak(page, credentials)
    ] as const
  ]

  describe.each(initConfigurations)(`Interactions with %s`, (_name, login) => {
    test('Logout leads back to login page', async () => {
      await login(page)
      const header = new CitizenHeader(page)
      await header.logout()
      await page.findByDataQa('weak-login').waitUntilVisible()
      await page.findByDataQa('strong-login').waitUntilVisible()
    })
  })
})
