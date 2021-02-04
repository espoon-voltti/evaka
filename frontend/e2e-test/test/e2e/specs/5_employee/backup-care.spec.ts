// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { daycareGroupFixture } from '../../dev-api/fixtures'
import { createBackupCareFixture } from '../../dev-api/fixtures'
import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import UnitPage, {
  daycareGroupElement,
  daycareGroupPlacementElement,
  missingPlacementElement
} from '../../pages/employee/units/unit-page'
import { logConsoleMessages } from '../../utils/fixture'
import GroupPlacementModal from '../../pages/employee/units/group-placement-modal'
import { ApplicationPersonDetail, BackupCare } from '../../dev-api/types'
import {
  insertBackupCareFixtures,
  insertDaycareGroupFixtures
} from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import { formatISODateString } from '../../utils/dates'
import LocalDate from '@evaka/lib-common/src/local-date'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const unitPage = new UnitPage()
const groupPlacementModal = new GroupPlacementModal()
let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let childFixture: ApplicationPersonDetail
let backupCareFixture: BackupCare

fixture('Employee - Backup care')
  .meta({ type: 'regression', subType: 'backup-care' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    childFixture = fixtures.enduserChildFixtureKaarina
    backupCareFixture = createBackupCareFixture(
      childFixture.id,
      fixtures.daycareFixture.id
    )
    await insertDaycareGroupFixtures([daycareGroupFixture])
    await insertBackupCareFixtures([backupCareFixture])
  })
  .beforeEach(async (t) => {
    await t.useRole(seppoAdminRole)
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
  })

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
    .eql('01.02.2021 - 03.02.2021')
  await t
    .expect(row.groupMissingDuration.textContent)
    .eql('01.02.2021 - 03.02.2021')
  await t.expect(row.addToGroupBtn.visible).ok()
})

test('backup care child can be placed into a group and removed from it', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()

  await unitPage.selectPeriodYear()
  await unitPage.openGroups()

  await unitPage.setFilterStartDate(LocalDate.of(2021, 1, 1))

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
    .eql('01.02.2021 - 03.02.2021')

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
    .eql('01.02.2021 - 03.02.2021')
  await t
    .expect(missingPlacement2.groupMissingDuration.textContent)
    .eql('01.02.2021 - 03.02.2021')
  await t.expect(group.groupPlacementRows.count).eql(0)
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})
