// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { newBrowserContext } from 'e2e-playwright/browser'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import config from 'e2e-test-common/config'
import {
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { Page } from 'playwright'
import UnitsPage from 'e2e-playwright/pages/employee/units/units'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import UnitPage from 'e2e-playwright/pages/employee/units/unit'
import { Fixture, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import { waitUntilEqual } from 'e2e-playwright/utils'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
let staffId: UUID
const groupId: UUID = uuidv4()

const tauno: EmployeeDetail = {
  email: 'tauno.testimies@example.com',
  firstName: 'Tauno',
  lastName: 'Testimies',
  roles: []
}

beforeEach(async () => {
  await resetDatabase()

  const [fixtures] = await initializeAreaAndPersonData()
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
  staffId = await insertEmployeeFixture(tauno)

  page = await (await newBrowserContext()).newPage()
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  await nav.login('UNIT_SUPERVISOR')
  await nav.openTab('units')
  const units = new UnitsPage(page)
  await units.navigateToUnit(fixtures.daycareFixture.id)
  unitPage = new UnitPage(page)
})

describe('Employee - unit ACL', () => {
  test('Staff can be assigned/removed to/from groups', async () => {
    async function toggleGroups() {
      const row = await (
        await unitInfo.staffAcl.getRow('Tauno Testimies')
      ).edit()
      await row.toggleStaffGroups([groupId])
      await row.save()
    }

    const expectedRow = {
      name: `${tauno.firstName} ${tauno.lastName}`,
      email: tauno.email!, // eslint-disable-line
      groups: []
    }

    const unitInfo = await unitPage.openUnitInformation()
    await unitInfo.staffAcl.addEmployeeAcl(staffId)
    await waitUntilEqual(() => unitInfo.staffAcl.rows, [expectedRow])
    await toggleGroups()
    await waitUntilEqual(() => unitInfo.staffAcl.rows, [
      { ...expectedRow, groups: ['Testailijat'] }
    ])
    await toggleGroups()
    await waitUntilEqual(() => unitInfo.staffAcl.rows, [expectedRow])
  })
})
