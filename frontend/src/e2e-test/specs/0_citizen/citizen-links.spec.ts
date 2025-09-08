// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { testAdult } from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

describe('Citizen links', () => {
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

  describe('without login', () => {
    test('accessibility page can be accessed', async () => {
      await page.goto(`${config.enduserUrl}/accessibility`)
      await page.findByDataQa('accessibility-statement').waitUntilVisible()
    })
  })

  describe.each(initConfigurations)(`Interactions with %s`, (_name, login) => {
    test('accessibility page can be accessed', async () => {
      await login(page)
      await page.goto(`${config.enduserUrl}/accessibility`)
      await page.findByDataQa('accessibility-statement').waitUntilVisible()
    })
  })
})
