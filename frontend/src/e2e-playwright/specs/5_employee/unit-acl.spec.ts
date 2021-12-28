// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import config from 'e2e-test-common/config'
import { resetDatabase } from 'e2e-test-common/dev-api'
import UnitsPage from 'e2e-playwright/pages/employee/units/units'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import { UnitPage } from 'e2e-playwright/pages/employee/units/unit'
import { Fixture, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import { waitUntilEqual, waitUntilSuccess } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { Page } from '../../utils/page'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
let staffId: UUID
const groupId: UUID = uuidv4()

const taunoFirstname = 'Tauno'
const taunoLastname = 'Testimies'
const taunoName = `${taunoFirstname} ${taunoLastname}`
const taunoEmail = 'tauno.testimies@example.com'
const tauno: Omit<EmployeeDetail, 'id' | 'externalId'> = {
  email: taunoEmail,
  firstName: taunoFirstname,
  lastName: taunoLastname,
  roles: []
}

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await Fixture.employee()
    .with({
      externalId: `espoo-ad:${config.unitSupervisorAad}`,
      roles: []
    })
    .withDaycareAcl(fixtures.daycareFixture.id, 'UNIT_SUPERVISOR')
    .save()
  await Fixture.daycareGroup()
    .with({
      id: groupId,
      daycareId: fixtures.daycareFixture.id,
      name: 'Testailijat'
    })
    .save()
  staffId = await Fixture.employee()
    .with(tauno)
    .save()
    .then(({ data: { id } }) => id)

  page = await Page.open()
  await employeeLogin(page, 'UNIT_SUPERVISOR')
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  await nav.openTab('units')
  const units = new UnitsPage(page)
  await units.navigateToUnit(fixtures.daycareFixture.id)
  unitPage = new UnitPage(page)
})

describe('Employee - unit ACL', () => {
  test('Staff can be assigned/removed to/from groups', async () => {
    async function toggleGroups() {
      const row = await waitUntilSuccess(() =>
        unitInfo.staffAcl.getRow(taunoName)
      )
      const rowEditor = await row.edit()
      await rowEditor.toggleStaffGroups([groupId])
      await rowEditor.save()
    }

    const expectedRow = {
      name: `${tauno.firstName} ${tauno.lastName}`,
      email: taunoEmail,
      groups: []
    }
    const unitInfo = await unitPage.openUnitInformation()
    await unitInfo.staffAcl.addEmployeeAcl(taunoEmail, staffId)
    await waitUntilEqual(() => unitInfo.staffAcl.rows, [expectedRow])
    await toggleGroups()
    await waitUntilEqual(
      () => unitInfo.staffAcl.rows,
      [{ ...expectedRow, groups: ['Testailijat'] }]
    )
    await toggleGroups()
    await waitUntilEqual(() => unitInfo.staffAcl.rows, [expectedRow])
  })
})
