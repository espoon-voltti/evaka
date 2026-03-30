// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { testAdult } from '../../dev-api/fixtures'
import { Fixture } from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { test, expect } from '../../playwright'
import { Page } from '../../utils/page'
import {
  employeeSfiLoginForm,
  enduserLoginSfi,
  enduserLoginWeak
} from '../../utils/user'

test.describe('SFI authentication', () => {
  let ssnEmployee: DevEmployee
  let citizenTab: Page

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    citizenTab = evaka
    await testAdult.saveAdult({
      updateMockVtjWithDependants: []
    })
    ssnEmployee = await Fixture.employee({
      firstName: testAdult.firstName,
      lastName: testAdult.lastName
    })
      .ssnEmployee(testAdult.ssn!)
      .save()
  })

  test('Citizen SFI logout leads back to login page', async () => {
    await enduserLoginSfi(citizenTab, testAdult)
    const header = new CitizenHeader(citizenTab)
    await header.logout()
    await expect(citizenTab.findByDataQa('weak-login')).toBeVisible()
    await expect(citizenTab.findByDataQa('strong-login')).toBeVisible()
  })

  test('Citizen weak logout leads back to login page', async () => {
    const credentials = {
      username: 'test@example.com',
      password: 'TestPassword456!'
    }
    await upsertWeakCredentials({
      id: testAdult.id,
      body: credentials
    })
    await enduserLoginWeak(citizenTab, credentials)
    const header = new CitizenHeader(citizenTab)
    await header.logout()
    await expect(citizenTab.findByDataQa('weak-login')).toBeVisible()
    await expect(citizenTab.findByDataQa('strong-login')).toBeVisible()
  })

  test('SFI logout invalidates all SFI sessions for the user', async () => {
    // Login to both SFIs and logout from citizen SFI
    await enduserLoginSfi(citizenTab, testAdult)
    const employeeTab = await Page.openNewTab(citizenTab)
    await employeeTab.goto(
      `${config.apiUrl}/employee/auth/sfi/login?RelayState=%2Femployee`
    )
    await employeeTab.find('[type=submit]').findText('Jatka').click()
    await expect(employeeTab.findByDataQa('username')).toBeVisible()
    const header = new CitizenHeader(citizenTab)
    await header.logout()

    // Verify that the employee SFI session has been logged out
    await employeeTab.findByDataQa('header').click()
    await expect(
      employeeTab.findByDataQa('session-expired-modal')
    ).toBeVisible()

    // Login again to both SFIs and logout from employee SFI
    await employeeSfiLoginForm(employeeTab, ssnEmployee)
    await citizenTab.goto(
      `${config.apiUrl}/citizen/auth/sfi/login?RelayState=%2F`
    )
    await citizenTab.find('[type=submit]').findText('Jatka').click()
    await expect(citizenTab.findByDataQa('header-city-logo')).toBeVisible()
    await employeeTab.findByDataQa('username').click()
    await employeeTab.findByDataQa('logout-btn').click()

    // Verify that the citizen SFI session has been logged out
    await citizenTab.findByDataQa('desktop-nav').click()
    await citizenTab.bringToFront()
    await expect(citizenTab.findByDataQa('session-expired-modal')).toBeVisible()
  })
})
