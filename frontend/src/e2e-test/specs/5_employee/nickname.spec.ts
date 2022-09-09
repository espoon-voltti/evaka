// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import { Fixture } from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import { EmployeeNickname } from '../../pages/employee/nickname'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: EmployeeDetail
let page: Page
let nav: EmployeeNav
let nicknamePage: EmployeeNickname
const firstName = 'Matti-Teppo Seppo'

beforeEach(async () => {
  await resetDatabase()
  admin = (
    await Fixture.employeeAdmin()
      .with({
        firstName
      })
      .save()
  ).data

  page = await Page.open()
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  nicknamePage = new EmployeeNickname(page)
  await nav.openAndClickDropdownMenuItem('nickname')
})

describe('Employee nickname', () => {
  test('nickname can be set', async () => {
    await nicknamePage.assertSelectedNickname('Matti-Teppo')
    await nicknamePage.assertAvailableNicknames([
      'Matti-Teppo',
      'Matti',
      'Teppo',
      'Seppo'
    ])

    await nicknamePage.selectNickname('Teppo')
    await nicknamePage.confirm()
    await waitUntilEqual(
      () => page.findByDataQa('username').textContent,
      'Teppo Sorsa'
    )

    await nicknamePage.assertSelectedNickname('Teppo')
  })
})
