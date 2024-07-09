// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import { EmployeesPage } from '../../pages/employee/employees'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let nav: EmployeeNav
let employeesPage: EmployeesPage

beforeEach(async () => {
  await resetServiceState()
  const admin = await Fixture.employeeAdmin().save()
  await Fixture.employeeServiceWorker()
    .with({ firstName: 'Teppo', lastName: 'Testaaja' })
    .save()

  page = await Page.open()
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  employeesPage = new EmployeesPage(page)
})

describe('Employees page', () => {
  beforeEach(async () => {
    await nav.openAndClickDropdownMenuItem('employees')
  })

  test('users can be searched by name', async () => {
    await waitUntilEqual(
      () => employeesPage.visibleUsers,
      ['Sorsa Seppo', 'Testaaja Teppo']
    )
    await employeesPage.nameInput.type('Test')
    await waitUntilEqual(() => employeesPage.visibleUsers, ['Testaaja Teppo'])
  })
})
