// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeHome from '../../pages/employee/home'
import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeFixture,
  insertEmployeeFixture,
  setAclForDaycares
} from '../../dev-api'
import { UUID } from '../../dev-api/types'
import UnitPage from '../../pages/employee/units/unit-page'
import { Role, t } from 'testcafe'

const home = new EmployeeHome()
const unitPage = new UnitPage()

const employeeAads = [
  'df979243-f081-4241-bc4f-e93a019bddfa',
  '7e7daa1e-2e92-4c36-9e90-63cea3cd8f3f'
] as const
let employeeUuids: UUID[] = []

const expectedAclRows = {
  seppo: { name: 'Seppo Sorsa', email: 'seppo.sorsa@espoo.fi' },
  pete: { name: 'Pete Päiväkoti', email: 'pete@example.com' },
  yrjo: { name: 'Yrjö Yksikkö', email: 'yy@example.com' }
}

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Employee - Unit ACL')
  .meta({ type: 'regression', subType: 'unit-acl' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await deleteEmployeeFixture(config.supervisorAad)
    await insertEmployeeFixture({
      aad: config.supervisorAad,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      email: 'seppo.sorsa@espoo.fi',
      roles: []
    })
    await setAclForDaycares(config.supervisorAad, fixtures.daycareFixture.id)
    employeeUuids = await Promise.all([
      insertEmployeeFixture({
        aad: employeeAads[0],
        firstName: 'Pete',
        lastName: 'Päiväkoti',
        email: 'pete@example.com',
        roles: []
      }),
      insertEmployeeFixture({
        aad: employeeAads[1],
        firstName: 'Yrjö',
        lastName: 'Yksikkö',
        email: 'yy@example.com',
        roles: []
      })
    ])
  })
  .beforeEach(async () => {
    await t.useRole(Role.anonymous())
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
    await Promise.all(employeeAads.map(deleteEmployeeFixture))
    await deleteEmployeeFixture(config.supervisorAad)
  })

test('User can add and delete unit supervisors', async (t) => {
  await home.login({
    aad: config.adminAad,
    roles: ['ADMIN', 'SERVICE_WORKER']
  })
  await home.navigateToUnits()
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await t
    .expect(await unitPage.supervisorAcl.getAclRows())
    .eql([expectedAclRows.seppo])
  await unitPage.supervisorAcl.addEmployeeAcl(employeeUuids[0])
  await t
    .expect(await unitPage.supervisorAcl.getAclRows())
    .eql([expectedAclRows.pete, expectedAclRows.seppo])
  await unitPage.supervisorAcl.addEmployeeAcl(employeeUuids[1])
  await t
    .expect(await unitPage.supervisorAcl.getAclRows())
    .eql([expectedAclRows.pete, expectedAclRows.seppo, expectedAclRows.yrjo])
  await unitPage.supervisorAcl.deleteEmployeeAclByIndex(0)
  await unitPage.supervisorAcl.deleteEmployeeAclByIndex(1)
  await t
    .expect(await unitPage.supervisorAcl.getAclRows())
    .eql([expectedAclRows.seppo])
})

test('User can add and delete staff', async (t) => {
  await home.login({
    aad: config.supervisorAad,
    roles: []
  })
  await home.navigateToUnits()
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await t.expect(await unitPage.staffAcl.getAclRows()).eql([])
  await unitPage.staffAcl.addEmployeeAcl(employeeUuids[0])
  await t
    .expect(await unitPage.staffAcl.getAclRows())
    .eql([expectedAclRows.pete])
  await unitPage.staffAcl.addEmployeeAcl(employeeUuids[1])
  await t
    .expect(await unitPage.staffAcl.getAclRows())
    .eql([expectedAclRows.pete, expectedAclRows.yrjo])
  await unitPage.staffAcl.deleteEmployeeAclByIndex(0)
  await unitPage.staffAcl.deleteEmployeeAclByIndex(0)
  await t.expect(await unitPage.staffAcl.getAclRows()).eql([])
})
