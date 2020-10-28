// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import UnitsPage from '../../pages/employee/units/units-page'
import UnitPage, {
  daycareGroupElement,
  daycareGroupPlacementElement,
  missingPlacementElement
} from '../../pages/employee/units/unit-page'
import GroupPlacementModal from '../../pages/employee/units/group-placement-modal'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { Child, DaycarePlacement } from '../../dev-api/types'
import {
  insertDaycareCaretakerFixtures,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures
} from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import { formatISODateString } from '../../utils/dates'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const unitsPage = new UnitsPage()
const unitPage = new UnitPage()
const groupPlacementModal = new GroupPlacementModal()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let childFixture: Child
let daycarePlacementFixture: DaycarePlacement

fixture('Employee - Units')
  .meta({ type: 'regression', subType: 'units' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await insertDaycareGroupFixtures([daycareGroupFixture])
    childFixture = fixtures.familyWithTwoGuardians.children[0]
    daycarePlacementFixture = createDaycarePlacementFixture(
      childFixture.id,
      fixtures.daycareFixture.id
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])
  })
  .beforeEach(async (t) => {
    await t.useRole(seppoAdminRole)
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
  })

test('filtering units, navigating to one, contact details', async (t) => {
  await unitsPage.filterByName(fixtures.daycareFixture.name)
  await t.expect(unitsPage.unitRows.count).eql(1)
  await unitsPage.navigateToNthUnit(0)
  await t
    .expect(unitPage.unitName.textContent)
    .eql(fixtures.daycareFixture.name)
  await t
    .expect(unitPage.visitingAddress.textContent)
    .eql(
      `${fixtures.daycareFixture.streetAddress}, ${fixtures.daycareFixture.postalCode} ${fixtures.daycareFixture.postOffice}`
    )
})

test('daycare has an empty group', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()
  await unitPage.openGroups()
  await t.expect(unitPage.groups.count).eql(1)
  const group = daycareGroupElement(unitPage.groups.nth(0))
  await t.expect(group.groupName.textContent).eql(daycareGroupFixture.name)
  await t.expect(group.groupFounded.textContent).eql('01.01.2000')
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})

test('daycare has one child missing group', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()
  await t.expect(unitPage.missingPlacementRows.count).eql(1)
  const row = missingPlacementElement(unitPage.missingPlacementRows.nth(0))
  await t
    .expect(row.childName.textContent)
    .eql(
      `${fixtures.familyWithTwoGuardians.children[0].lastName} ${fixtures.familyWithTwoGuardians.children[0].firstName}`
    )
  await t
    .expect(row.childDateOfBirth.textContent)
    .eql(
      formatISODateString(
        fixtures.familyWithTwoGuardians.children[0].dateOfBirth
      )
    )
  await t
    .expect(row.placementDuration.textContent)
    .eql('01.05.2020 - 31.08.2021')
  await t
    .expect(row.groupMissingDuration.textContent)
    .eql('01.05.2020 - 31.08.2021')
  await t.expect(row.addToGroupBtn.visible).ok()
})

test('child can be placed into a group and removed from it', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()
  await unitPage.openGroups()

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
    .eql(
      `${fixtures.familyWithTwoGuardians.children[0].lastName} ${fixtures.familyWithTwoGuardians.children[0].firstName}`
    )
  await t
    .expect(groupPlacement.placementDuration.textContent)
    .eql('01.05.2020 - 31.08.2021')

  // after removing the child is again visible at missing groups and no longer at the group
  await groupPlacement.remove()
  await t.expect(unitPage.missingPlacementRows.count).eql(1)
  const missingPlacement2 = missingPlacementElement(
    unitPage.missingPlacementRows.nth(0)
  )
  await t
    .expect(missingPlacement2.childName.textContent)
    .eql(
      `${fixtures.familyWithTwoGuardians.children[0].lastName} ${fixtures.familyWithTwoGuardians.children[0].firstName}`
    )
  await t
    .expect(missingPlacement2.placementDuration.textContent)
    .eql('01.05.2020 - 31.08.2021')
  await t
    .expect(missingPlacement2.groupMissingDuration.textContent)
    .eql('01.05.2020 - 31.08.2021')
  await t.expect(group.groupPlacementRows.count).eql(0)
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})

test('Unit occupancy rates cannot be determined when no caretaker', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)

  await t.expect(unitPage.occupancies('confirmed').noValidValues.exists).ok()
  await t.expect(unitPage.occupancies('planned').noValidValues.exists).ok()
})

test('Unit occupancy rates are correct with properly set caretaker counts', async (t) => {
  await insertDaycareCaretakerFixtures([
    {
      groupId: daycareGroupFixture.id,
      amount: 1.0,
      startDate: daycareGroupFixture.startDate
    }
  ])
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await t
    .expect(unitPage.occupancies('confirmed').maximum.textContent)
    .contains('14,3 %')
  await t
    .expect(unitPage.occupancies('confirmed').minimum.textContent)
    .contains('14,3 %')

  await t
    .expect(unitPage.occupancies('planned').maximum.textContent)
    .contains('14,3 %')
  await t
    .expect(unitPage.occupancies('planned').minimum.textContent)
    .contains('14,3 %')
})
