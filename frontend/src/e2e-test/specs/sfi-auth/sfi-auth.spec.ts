// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { testAdult } from '../../dev-api/fixtures'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { employeeSfiLogin, enduserLogin } from '../../utils/user'

beforeEach(async () => resetServiceState())

describe('SFI authentication', () => {
  let ssnEmployee: DevEmployee
  beforeEach(async () => {
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
  afterAll(async () => {
    await resetServiceState()
  })
  test('SFI logout invalidates all SFI sessions for the user', async () => {
    const citizenTab = await Page.open()

    // Login to both SFIs and logout from citizen SFI
    await enduserLogin(citizenTab, testAdult)
    const employeeTab = await Page.openNewTab(citizenTab)
    await employeeTab.goto(
      `${config.apiUrl}/employee/auth/sfi/login?RelayState=%2Femployee`
    )
    await employeeTab.find('[type=submit]').findText('Jatka').click()
    await employeeTab.findByDataQa('username').waitUntilVisible()
    const header = new CitizenHeader(citizenTab)
    await header.logout()

    // Verify that the employee SFI session has been logged out
    await employeeTab.findByDataQa('header').click()
    await employeeTab.findByDataQa('session-expired-modal').waitUntilVisible()

    // Login again to both SFIs and logout from employee SFI
    await employeeSfiLogin(employeeTab, ssnEmployee)
    await citizenTab.goto(
      `${config.apiUrl}/citizen/auth/sfi/login?RelayState=%2F`
    )
    await citizenTab.find('[type=submit]').findText('Jatka').click()
    await citizenTab.findByDataQa('header-city-logo').waitUntilVisible()
    await employeeTab.findByDataQa('username').click()
    await employeeTab.findByDataQa('logout-btn').click()

    // Verify that the citizen SFI session has been logged out
    await citizenTab.findByDataQa('desktop-nav').click()
    await citizenTab.bringToFront()
    await citizenTab.findByDataQa('session-expired-modal').waitUntilVisible()
  })
})
