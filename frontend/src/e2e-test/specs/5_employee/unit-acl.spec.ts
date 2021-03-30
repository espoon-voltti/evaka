// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeHome from '../../pages/employee/home'
import config from 'e2e-test-common/config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeFixture,
  deleteMobileDevice,
  deletePairing,
  insertEmployeeFixture,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { UUID } from 'e2e-test-common/dev-api/types'
import UnitPage from '../../pages/employee/units/unit-page'
import { Role, t } from 'testcafe'

const home = new EmployeeHome()
const unitPage = new UnitPage()

const employeeExternalIds = [
  'espoo-ad:df979243-f081-4241-bc4f-e93a019bddfa',
  'espoo-ad:7e7daa1e-2e92-4c36-9e90-63cea3cd8f3f'
] as const
let employeeUuids: UUID[] = []

let pairingId: UUID | undefined = undefined
let deviceId: UUID | null = null

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
    await deleteEmployeeFixture(config.supervisorExternalId)
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      email: 'seppo.sorsa@espoo.fi',
      roles: []
    })
    await setAclForDaycares(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
    employeeUuids = await Promise.all([
      insertEmployeeFixture({
        externalId: employeeExternalIds[0],
        firstName: 'Pete',
        lastName: 'Päiväkoti',
        email: 'pete@example.com',
        roles: []
      }),
      insertEmployeeFixture({
        externalId: employeeExternalIds[1],
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
  .afterEach(async (m) => {
    if (pairingId) {
      await deletePairing(pairingId)
      pairingId = undefined
    }
    if (deviceId) {
      await deleteMobileDevice(deviceId)
      deviceId = null
    }
    await logConsoleMessages(m)
  })
  .after(async () => {
    await cleanUp()
    await Promise.all(employeeExternalIds.map(deleteEmployeeFixture))
    await deleteEmployeeFixture(config.supervisorExternalId)
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

test('User can add and delete special education teachers', async (t) => {
  await home.login({
    aad: config.adminAad,
    roles: ['ADMIN', 'SERVICE_WORKER']
  })
  await home.navigateToUnits()
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await t.expect(await unitPage.specialEducationTeacherAcl.getAclRows()).eql([])
  await unitPage.specialEducationTeacherAcl.addEmployeeAcl(employeeUuids[0])
  await t
    .expect(await unitPage.specialEducationTeacherAcl.getAclRows())
    .eql([expectedAclRows.pete])
  await unitPage.specialEducationTeacherAcl.addEmployeeAcl(employeeUuids[1])
  await t
    .expect(await unitPage.specialEducationTeacherAcl.getAclRows())
    .eql([expectedAclRows.pete, expectedAclRows.yrjo])
  await unitPage.specialEducationTeacherAcl.deleteEmployeeAclByIndex(0)
  await unitPage.specialEducationTeacherAcl.deleteEmployeeAclByIndex(0)
  await t.expect(await unitPage.specialEducationTeacherAcl.getAclRows()).eql([])
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

test('User can add a mobile device unit side', async (t) => {
  await home.login({
    aad: config.supervisorAad,
    roles: []
  })
  await home.navigateToUnits()
  await unitPage.navigateHere(fixtures.daycareFixture.id)

  const { pairingId: pId, deviceId: dId } = await unitPage.addMobileDevice()
  pairingId = pId
  deviceId = dId

  await t.expect(unitPage.mobileDevicesTableRows.exists).ok()
})

test('Added mobile devices should not be listed in employee selector', async (t) => {
  await home.login({
    aad: config.supervisorAad,
    roles: []
  })
  await home.navigateToUnits()
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await t.expect(unitPage.mobileDevicesTableRows.exists).notOk()
  await t.expect(unitPage.staffAcl.addInput.exists).ok()
  await t.click(unitPage.staffAcl.addInput)
  const employeeCount = await unitPage.employeeOptions.count
  await t.pressKey('esc')

  const { pairingId: pId, deviceId: dId } = await unitPage.addMobileDevice()
  pairingId = pId
  deviceId = dId

  await home.navigateToUnits()
  await unitPage.navigateHere(fixtures.daycareFixture.id)

  await t.expect(unitPage.mobileDevicesTableRows.exists).ok()
  await t.click(unitPage.staffAcl.addInput)
  await t.expect(unitPage.employeeOptions.count).eql(employeeCount)
})
