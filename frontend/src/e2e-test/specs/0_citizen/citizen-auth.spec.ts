// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { testAdult } from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

const credentials = {
  username: 'test@example.com',
  password: 'TestPassword456!'
}

test.beforeEach(async () => {
  await resetServiceState()
  await testAdult.saveAdult({
    updateMockVtjWithDependants: []
  })
  await upsertWeakCredentials({
    id: testAdult.id,
    body: credentials
  })
})

test.describe('Citizen authentication - direct login', () => {
  const login = async (page: Page) => enduserLogin(page, testAdult)

  test('Logout leads back to login page', async ({ evaka }) => {
    await login(evaka)
    const header = new CitizenHeader(evaka)
    await header.logout()
    await evaka.findByDataQa('weak-login').waitUntilVisible()
    await evaka.findByDataQa('strong-login').waitUntilVisible()
  })
})

test.describe('Citizen authentication - weak login', () => {
  const login = async (page: Page) => enduserLoginWeak(page, credentials)

  test('Logout leads back to login page', async ({ evaka }) => {
    await login(evaka)
    const header = new CitizenHeader(evaka)
    await header.logout()
    await evaka.findByDataQa('weak-login').waitUntilVisible()
    await evaka.findByDataQa('strong-login').waitUntilVisible()
  })
})
