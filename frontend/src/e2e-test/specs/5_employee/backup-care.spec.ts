// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  createBackupCareFixture,
  daycareGroupFixture
} from 'e2e-test-common/dev-api/fixtures'
import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import UnitPage, {
  daycareGroupElement,
  daycareGroupPlacementElement,
  missingPlacementElement
} from '../../pages/employee/units/unit-page'
import { logConsoleMessages } from '../../utils/fixture'
import GroupPlacementModal from '../../pages/employee/units/group-placement-modal'
import { BackupCare, PersonDetail } from 'e2e-test-common/dev-api/types'
import {
  insertBackupCareFixtures,
  insertDaycareGroupFixtures,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoManager } from '../../config/users'
import { formatISODateString } from '../../utils/dates'
import LocalDate from 'lib-common/local-date'
import config from 'e2e-test-common/config'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const unitPage = new UnitPage()
const groupPlacementModal = new GroupPlacementModal()
let fixtures: AreaAndPersonFixtures
let childFixture: PersonDetail
let backupCareFixture: BackupCare

fixture('Employee - Backup care')
  .meta({ type: 'regression', subType: 'backup-care' })
  .beforeEach(async (t) => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
    childFixture = fixtures.enduserChildFixtureKaarina
    backupCareFixture = createBackupCareFixture(
      childFixture.id,
      fixtures.daycareFixture.id
    )
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      roles: []
    })
    await setAclForDaycares(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
    await insertDaycareGroupFixtures([daycareGroupFixture])
    await insertBackupCareFixtures([backupCareFixture])

    await employeeLogin(t, seppoManager, adminHome.homePage('manager'))
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)

test('daycare has one backup care child missing group', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()
  await unitPage.selectPeriodYear()
  await t.expect(unitPage.missingPlacementRows.count).eql(1)
  const row = missingPlacementElement(unitPage.missingPlacementRows.nth(0))
  await t
    .expect(row.childName.textContent)
    .eql(`${childFixture.lastName} ${childFixture.firstName}`)
  await t
    .expect(row.childDateOfBirth.textContent)
    .eql(formatISODateString(childFixture.dateOfBirth))
  await t
    .expect(row.placementDuration.textContent)
    .eql('01.02.2022 - 03.02.2022')
  await t
    .expect(row.groupMissingDuration.textContent)
    .eql('01.02.2022 - 03.02.2022')
  await t.expect(row.addToGroupBtn.visible).ok()
})

test('backup care child can be placed into a group and removed from it', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()

  await unitPage.selectPeriodYear()
  await unitPage.openGroups()

  await unitPage.setFilterStartDate(LocalDate.of(2022, 1, 1))

  // open the group placement modal and submit it with default values
  const missingPlacement = missingPlacementElement(
    unitPage.missingPlacementRows.nth(0)
  )
  await missingPlacement.addToGroup()
  await t.expect(groupPlacementModal.root.visible).ok()
  await groupPlacementModal.submit()

  // no more missing placements
  await t.expect(unitPage.missingPlacementRows.count).eql(0)

  // check child is listed in group
  const group = daycareGroupElement(unitPage.groups.nth(0))
  await t.expect(group.groupPlacementRows.count).eql(1)
  const groupPlacement = daycareGroupPlacementElement(
    group.groupPlacementRows.nth(0)
  )
  await t
    .expect(groupPlacement.childName.textContent)
    .eql(`${childFixture.lastName} ${childFixture.firstName}`)
  await t
    .expect(groupPlacement.placementDuration.textContent)
    .eql('01.02.2022- 03.02.2022')

  // after removing the child is again visible at missing groups and no longer at the group
  await groupPlacement.remove()
  await t.expect(unitPage.missingPlacementRows.count).eql(1)
  const missingPlacement2 = missingPlacementElement(
    unitPage.missingPlacementRows.nth(0)
  )
  await t
    .expect(missingPlacement2.childName.textContent)
    .eql(`${childFixture.lastName} ${childFixture.firstName}`)
  await t
    .expect(missingPlacement2.placementDuration.textContent)
    .eql('01.02.2022 - 03.02.2022')
  await t
    .expect(missingPlacement2.groupMissingDuration.textContent)
    .eql('01.02.2022 - 03.02.2022')
  await t.expect(group.groupPlacementRows.count).eql(0)
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})
