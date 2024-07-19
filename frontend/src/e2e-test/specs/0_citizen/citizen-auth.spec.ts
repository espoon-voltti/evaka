// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { startTest } from '../../browser'
import { testAdult, Fixture } from '../../dev-api/fixtures'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { KeycloakRealmClient } from '../../utils/keycloak'
import { Page } from '../../utils/page'
import {
  CitizenWeakAccount,
  citizenWeakAccount,
  enduserLogin,
  enduserLoginWeak
} from '../../utils/user'

describe('Citizen authentication', () => {
  let page: Page
  let account: CitizenWeakAccount

  beforeEach(async () => {
    await startTest()
    await Fixture.person(testAdult).saveAdult({
      updateMockVtjWithDependants: []
    })
    const keycloak = await KeycloakRealmClient.createCitizenClient()
    await keycloak.deleteAllUsers()

    account = citizenWeakAccount(testAdult)
    await keycloak.createUser({ ...account, enabled: true })

    page = await Page.open()
  })

  const initConfigurations = [
    [
      'direct login',
      async (page: Page) => enduserLogin(page, testAdult)
    ] as const,
    [
      'weak login',
      async (page: Page) => enduserLoginWeak(page, account)
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
