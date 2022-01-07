// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import { resetDatabase } from 'e2e-test-common/dev-api'
import { Fixture } from 'e2e-test-common/dev-api/fixtures'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import { EmployeePinPage } from '../../pages/employee/employee-pin'
import { Page } from '../../utils/page'

let admin: EmployeeDetail
let page: Page
let nav: EmployeeNav
let pinPage: EmployeePinPage

beforeEach(async () => {
  await resetDatabase()
  admin = (await Fixture.employeeAdmin().save()).data

  page = await Page.open()
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  pinPage = new EmployeePinPage(page)
})

describe('Employees PIN', () => {
  beforeEach(async () => {
    await nav.openAndClickDropdownMenuItem('pinCode')
  })

  test('shows a warning if PIN is too easy, and warning disappears once PIN is valid', async () => {
    await pinPage.pinInput.fill('1111')
    await waitUntilEqual(
      () => pinPage.inputInfo.innerText,
      'Liian helppo PIN-koodi tai PIN-koodi sisältää kirjaimia'
    )

    await pinPage.pinInput.fill('9128')
    await pinPage.inputInfo.waitUntilHidden()
  })

  test('shows a warning if PIN is locked, and warning disappears when new PIN is set', async () => {
    await Fixture.employeePin()
      .with({
        userId: undefined,
        employeeExternalId: admin.externalId,
        pin: '2580',
        locked: true
      })
      .save()

    await page.reload()
    await pinPage.pinLockedAlertBox.waitUntilVisible()
    await pinPage.pinInput.type('2580')
    await pinPage.pinSendButton.click()
    await pinPage.pinLockedAlertBox.waitUntilHidden()
  })
})
