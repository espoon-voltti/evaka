// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import { EmployeesPage } from '../../pages/employee/employees'
import { expect, test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

test.describe('Employees page', () => {
  let page: Page
  let nav: EmployeeNav
  let employeesPage: EmployeesPage
  let admin: DevEmployee
  let employee: DevEmployee

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    admin = await Fixture.employee({
      firstName: 'Seppo',
      lastName: 'Sorsa'
    })
      .admin()
      .save()
    employee = await Fixture.employee({
      firstName: 'Teppo',
      lastName: 'Testaaja'
    })
      .serviceWorker()
      .save()

    page = evaka
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    nav = new EmployeeNav(page)
    employeesPage = new EmployeesPage(page)
  })

  test.describe('Employee list', () => {
    test.beforeEach(async () => {
      await nav.openAndClickDropdownMenuItem('employees')
    })

    test('users can be searched by name', async () => {
      await expect(employeesPage.employeeNames).toHaveText([
        'Sorsa Seppo',
        'Testaaja Teppo'
      ])

      await employeesPage.clickDeactivatedEmployees()
      await employeesPage.deactivateEmployee(0)
      await expect(employeesPage.employeeNames).toHaveText([
        'Sorsa Seppo',
        'Testaaja Teppo'
      ])

      await employeesPage.clickDeactivatedEmployees()
      await expect(employeesPage.employeeNames).toHaveText(['Testaaja Teppo'])

      await employeesPage.clickDeactivatedEmployees()
      await employeesPage.activateEmployee(0)
      await expect(employeesPage.employeeNames).toHaveText([
        'Sorsa Seppo',
        'Testaaja Teppo'
      ])

      await employeesPage.nameInput.fill('Test')
      await expect(employeesPage.employeeNames).toHaveText(['Testaaja Teppo'])
    })

    test('can navigate to employee page', async () => {
      const employeePage = await employeesPage.openEmployeePage(employee)
      await employeePage.content
        .findTextExact(`${employee.firstName} ${employee.lastName}`)
        .waitUntilVisible()
      await employeePage.content
        .findTextExact(employee.email!)
        .waitUntilVisible()
    })

    test('a new employee can be added with SSN and deleted', async () => {
      const person = {
        ssn: '010107A977S',
        firstName: 'Erkki',
        lastName: 'Esimerkki',
        email: 'test@example.com'
      }
      await employeesPage.createNewSsnEmployee.click()

      const wizard = employeesPage.createSsnEmployeeWizard
      await wizard.ssn.fill(person.ssn)
      await wizard.firstName.fill(person.firstName)
      await wizard.lastName.fill(person.lastName)
      await wizard.email.fill(person.email)
      await wizard.ok.click()
      await wizard.waitUntilHidden()

      await nav.openAndClickDropdownMenuItem('employees')
      await expect(employeesPage.employeeNames).toHaveText([
        'Esimerkki Erkki',
        'Sorsa Seppo',
        'Testaaja Teppo'
      ])
      await employeesPage.deleteEmployee(0)
      await expect(employeesPage.employeeNames).toHaveText([
        'Sorsa Seppo',
        'Testaaja Teppo'
      ])
    })
  })
})
