// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { newBrowserContext } from 'e2e-playwright/browser'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import { waitUntilEqual } from 'e2e-playwright/utils'
import config from 'e2e-test-common/config'
import { resetDatabase } from 'e2e-test-common/dev-api'
import { Page } from 'playwright'
import { EmployeePinPage } from '../../pages/employee/employee-pin'
import { Fixture } from '../../../e2e-test-common/dev-api/fixtures'
import { employeeLogin } from 'e2e-playwright/utils/user'

let page: Page
let nav: EmployeeNav
let pinPage: EmployeePinPage

beforeEach(async () => {
  await resetDatabase()

  page = await (await newBrowserContext()).newPage()
  await employeeLogin(page, 'ADMIN')
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
    await waitUntilEqual(() => pinPage.inputInfo.visible, false)
  })

  test('shows a warning if PIN is locked, and warning disappears when new PIN is set', async () => {
    await Fixture.employeePin()
      .with({
        userId: undefined,
        employeeExternalId: config.adminExternalId,
        pin: '2580',
        locked: true
      })
      .save()

    await page.reload()
    await waitUntilEqual(() => pinPage.pinLockedAlertBox.visible, true)
    await pinPage.pinInput.type('2580')
    await pinPage.pinSendButton.click()
    await waitUntilEqual(() => pinPage.pinLockedAlertBox.visible, false)
  })
})
