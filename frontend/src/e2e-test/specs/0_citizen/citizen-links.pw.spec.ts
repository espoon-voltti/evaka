// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { testAdult } from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

test.describe('Citizen links', () => {
  let page: Page
  const credentials = {
    username: 'test@example.com',
    password: 'TestPassword456!'
  }

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await testAdult.saveAdult({
      updateMockVtjWithDependants: []
    })
    await upsertWeakCredentials({
      id: testAdult.id,
      body: credentials
    })

    page = evaka
  })

  test.describe('without login', () => {
    test('accessibility page can be accessed', async () => {
      await page.goto(`${config.enduserUrl}/accessibility`)
      await page.findByDataQa('accessibility-statement').waitUntilVisible()
    })
  })

  test.describe('Interactions with direct login', () => {
    test('accessibility page can be accessed', async () => {
      await enduserLogin(page, testAdult)
      await page.goto(`${config.enduserUrl}/accessibility`)
      await page.findByDataQa('accessibility-statement').waitUntilVisible()
    })
  })

  test.describe('Interactions with weak login', () => {
    test('accessibility page can be accessed', async () => {
      await enduserLoginWeak(page, credentials)
      await page.goto(`${config.enduserUrl}/accessibility`)
      await page.findByDataQa('accessibility-statement').waitUntilVisible()
    })
  })
})
