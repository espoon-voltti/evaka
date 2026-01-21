// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { testAdult } from '../../dev-api/fixtures'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { EmployeesPage } from '../../pages/employee/employees'
import { Page } from '../../utils/page'
import { employeeLogin, employeeSfiLogin, enduserLogin } from '../../utils/user'

beforeEach(async () => resetServiceState())

describe('SFI authentication', () => {
  let admin: DevEmployee
  beforeEach(async () => {
    await testAdult.saveAdult({
      updateMockVtjWithDependants: []
    })
    admin = await Fixture.employee({
      firstName: 'Test',
      lastName: 'Tester'
    })
      .admin()
      .save()
  })
  afterAll(async () => {
    await resetServiceState()
  })
  test('SFI logout invalidates all SFI sessions for the user', async () => {
    const employeeAdPage = await Page.open()
    const citizenSfiPage = await Page.open()
    const employeeSfiPage = await Page.open()

    await employeeLogin(employeeAdPage, admin)
    await employeeAdPage.goto(config.employeeUrl + '/employees')
    const employeesPage = new EmployeesPage(employeeAdPage)
    await employeesPage.createNewSsnEmployee.click()
    const wizard = employeesPage.createSsnEmployeeWizard
    await wizard.ssn.fill(testAdult.ssn!)
    await wizard.firstName.fill(testAdult.firstName)
    await wizard.lastName.fill(testAdult.lastName)
    await wizard.email.fill('test@example.com')
    await wizard.ok.click()
    await wizard.waitUntilHidden()

    // Login to both SFIs and logout from citizen SFI
    await enduserLogin(citizenSfiPage, testAdult)
    await employeeSfiLogin(employeeSfiPage, testAdult)
    const header = new CitizenHeader(citizenSfiPage)
    await header.logout()

    // Verify that the employee SFI session has been logged out
    await employeeSfiPage.findByDataQa('header').click()
    await employeeSfiPage
      .findByDataQa('session-expired-modal')
      .waitUntilVisible()
    await employeeSfiPage
      .findByDataQa('session-expired-modal')
      .findText('Peruuta')
      .click()

    // Login again to both SFIs and logout from employee SFI
    await employeeSfiPage.goto(
      `${config.apiUrl}/employee/auth/sfi/login?RelayState=%2Femployee`
    )
    await employeeSfiPage.find('[type=submit]').findText('Jatka').click()
    await employeeSfiPage.findByDataQa('username').waitUntilVisible()
    await enduserLogin(citizenSfiPage, testAdult)
    await employeeSfiPage.findByDataQa('username').click()
    await employeeSfiPage.findByDataQa('logout-btn').click()

    // Verify that the citizen SFI session has been logged out
    await citizenSfiPage.findByDataQa('desktop-nav').click()
    await citizenSfiPage
      .findByDataQa('session-expired-modal')
      .waitUntilVisible()
  })
})
