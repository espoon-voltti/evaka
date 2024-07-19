// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { startTest } from '../../browser'
import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import EmployeeNav from '../../pages/employee/employee-nav'
import { EmployeesPage } from '../../pages/employee/employees'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let nav: EmployeeNav
let employeesPage: EmployeesPage

beforeEach(async () => {
  await startTest()
  const admin = await Fixture.employee({
    firstName: 'Seppo',
    lastName: 'Sorsa'
  })
    .admin()
    .save()
  await Fixture.employee({ firstName: 'Teppo', lastName: 'Testaaja' })
    .serviceWorker()
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
