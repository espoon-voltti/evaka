// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import config from 'e2e-test-common/config'
import {
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import UnitsPage from 'e2e-playwright/pages/employee/units/units'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import UnitPage from 'e2e-playwright/pages/employee/units/unit'
import { Fixture, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { Page } from '../../utils/page'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
const groupId: UUID = uuidv4()

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertEmployeeFixture({
    externalId: `espoo-ad:${config.unitSupervisorAad}`,
    email: 'teppo.testaaja@example.com',
    firstName: 'Teppo',
    lastName: 'Testaaja',
    roles: []
  })
  await Fixture.daycareGroup()
    .with({
      id: groupId,
      daycareId: fixtures.daycareFixture.id,
      name: 'Testailijat'
    })
    .save()
  await setAclForDaycares(
    `espoo-ad:${config.unitSupervisorAad}`,
    fixtures.daycareFixture.id
  )

  page = await Page.open()
  await employeeLogin(page, 'UNIT_SUPERVISOR')
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  await nav.openTab('units')
  const units = new UnitsPage(page)
  await units.navigateToUnit(fixtures.daycareFixture.id)
  unitPage = new UnitPage(page)
})

describe('Employee - unit view', () => {
  test('Open groups stay open when reloading page', async () => {
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await page.reload()
    await groupsSection.assertGroupCollapsibleIsOpen(groupId)
  })

  test('Open groups stay open when visiting other unit page tab', async () => {
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await unitPage.openUnitInformation()
    await unitPage.openGroups()
    await groupsSection.assertGroupCollapsibleIsOpen(groupId)
  })
})
