// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import ChildInformationPage from '../../pages/employee/child-information/child-information-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { t } from 'testcafe'
import { DaycarePlacement } from '../../dev-api/types'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures
} from '../../dev-api'
import { seppoAdminRole } from '../../config/users'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const childInformation = new ChildInformationPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let daycarePlacementFixture: DaycarePlacement

fixture('Employee - Child Information')
  .meta({ type: 'regression', subType: 'childinformation' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

    await insertDaycareGroupFixtures([daycareGroupFixture])
    daycarePlacementFixture = createDaycarePlacementFixture(
      fixtures.familyWithTwoGuardians.children[0].id,
      fixtures.daycareFixture.id
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])
  })
  .beforeEach(async () => {
    await t.useRole(seppoAdminRole)
    await employeeHome.navigateToChildInformation(
      fixtures.familyWithTwoGuardians.children[0].id
    )
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
  })

test('create service need for a child can be added and removed', async () => {
  await childInformation.openServiceNeedCollapsible()
  await childInformation.verifyNumberOfServiceNeeds(0)
  await childInformation.openServiceNeedForm()
  await childInformation.verifyServiceNeedDefaultValues()
  await childInformation.createNewServiceNeed()
  await childInformation.verifyNumberOfServiceNeeds(1)
  await childInformation.verifyServiceNeedDetails(0)
  await childInformation.removeServiceNeed(0)
  await childInformation.verifyNumberOfServiceNeeds(0)
})

test('backup care for a child can be added and removed', async () => {
  await childInformation.openBackupCaresCollapsible()
  await childInformation.createBackupCare(
    fixtures.daycareFixture,
    '01.02.2020',
    '03.02.2020'
  )
  await t.expect(await childInformation.getBackupCares()).eql([
    {
      unit: fixtures.daycareFixture.name,
      period: '01.02.2020 - 03.02.2020'
    }
  ])
  await childInformation.deleteBackupCare(0)
  await t.expect(await childInformation.getBackupCares()).eql([])
})

test('guardian information is shown', async () => {
  await childInformation.openGuardiansCollapsible()
  await t.expect(childInformation.guardianRows.exists).ok()
  await t
    .expect(
      childInformation.findGuardianRow(
        fixtures.familyWithTwoGuardians.guardian.ssn
      ).visible
    )
    .ok()
})
