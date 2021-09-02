// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { newBrowserContext } from 'e2e-playwright/browser'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import { insertEmployeeFixture, resetDatabase } from 'e2e-test-common/dev-api'
import { Page } from 'playwright'
import { EmployeesPage } from '../../pages/employee/employees'

let page: Page
let nav: EmployeeNav
let employeesPage: EmployeesPage

beforeEach(async () => {
  await resetDatabase()

  await insertEmployeeFixture({
    email: 'teppo.testaaja@example.com',
    firstName: 'Teppo',
    lastName: 'Testaaja',
    roles: ['SERVICE_WORKER']
  })

  page = await (await newBrowserContext()).newPage()
  await employeeLogin(page, 'ADMIN')
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
