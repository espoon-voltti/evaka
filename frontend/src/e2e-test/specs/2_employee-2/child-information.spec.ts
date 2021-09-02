// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeHome from '../../pages/employee/home'
import ChildInformationPage from '../../pages/employee/child-information/child-information-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { t } from 'testcafe'
import { DaycarePlacement } from 'e2e-test-common/dev-api/types'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'

const employeeHome = new EmployeeHome()
const childInformation = new ChildInformationPage()

let fixtures: AreaAndPersonFixtures
let daycarePlacementFixture: DaycarePlacement

fixture('Employee - Child Information')
  .meta({ type: 'regression', subType: 'childinformation' })
  .beforeEach(async (t) => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()

    await insertDaycareGroupFixtures([daycareGroupFixture])
    daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.familyWithTwoGuardians.children[0].id,
      fixtures.daycareFixture.id
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])

    await employeeLogin(t, seppoAdmin)
    await employeeHome.navigateToChildInformation(
      fixtures.familyWithTwoGuardians.children[0].id
    )
  })
  .afterEach(logConsoleMessages)

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

test('backup pickups can be added and deleted', async () => {
  const name = 'Mikko Mallikas'
  const phone = '123456'
  await childInformation.openFamilyContactsCollapsible()
  await t.expect(childInformation.backupPickupRows(name).exists).notOk()
  await childInformation.addBackupPickup(name, phone)
  await t.expect(childInformation.backupPickupRows(name).visible).ok()
  await childInformation.deleteBackupPickup(name)
  await t.expect(childInformation.backupPickupRows(name).exists).notOk()
})

test('medication info and be added and removed', async () => {
  const medication = 'Epipen'

  await t.expect(childInformation.medication.textContent).eql('')

  await t.click(childInformation.editChildBtn)
  await t.typeText(childInformation.medicationInput, medication)
  await t.click(childInformation.confirmEditedChildBtn)
  await t.expect(childInformation.medication.textContent).eql(medication)

  await t.click(childInformation.editChildBtn)
  await t.selectText(childInformation.medicationInput).pressKey('delete')
  await t.click(childInformation.confirmEditedChildBtn)
  await t.expect(childInformation.medication.textContent).eql('')
})
