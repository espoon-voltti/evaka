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
} from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { Child, DaycarePlacement } from 'e2e-test-common/dev-api/types'
import {
  insertDaycareCaretakerFixtures,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertServiceNeedOptions,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import { formatISODateString } from '../../utils/dates'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const unitsPage = new UnitsPage()
const unitPage = new UnitPage()
const groupPlacementModal = new GroupPlacementModal()

let fixtures: AreaAndPersonFixtures
let childFixture: Child
let daycarePlacementFixture: DaycarePlacement

fixture('Employee - Units')
  .meta({ type: 'regression', subType: 'units' })
  .beforeEach(async (t) => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
    await insertServiceNeedOptions()
    await insertDaycareGroupFixtures([daycareGroupFixture])
    childFixture = fixtures.familyWithTwoGuardians.children[0]
    daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      childFixture.id,
      fixtures.daycareFixture.id
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])

    await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)

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
  await t.expect(group.groupStartDate.textContent).eql('01.01.2000')
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})

test('Unit group name, start date and end date can all be updated', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabGroups()

  await t.expect(unitPage.groups.count).eql(1)
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await t.click(group.groupUpdateBtn)
  await t.selectText(unitPage.groupUpdateModal.nameInput)
  await t.typeText(unitPage.groupUpdateModal.nameInput, 'Uusi nimi')
  await t.selectText(unitPage.groupUpdateModal.startDateInput)
  await t.typeText(unitPage.groupUpdateModal.startDateInput, '01.01.2020')
  await t.pressKey('tab esc')
  await t.selectText(unitPage.groupUpdateModal.endDateInput)
  await t.typeText(unitPage.groupUpdateModal.endDateInput, '31.12.2022')
  await t.pressKey('tab esc')
  await t.click(unitPage.groupUpdateModal.submit)

  await t.click(group.root)
  await t.expect(group.groupName.textContent).eql('Uusi nimi')
  await t.expect(group.groupStartDate.textContent).eql('01.01.2020')
  await t.expect(group.groupEndDate.textContent).eql('31.12.2022')
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
    .eql('01.05.2021 - 31.08.2022')
  await t
    .expect(row.groupMissingDuration.textContent)
    .eql('01.05.2021 - 31.08.2022')
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
    .eql('01.05.2021- 31.08.2022')

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
    .eql('01.05.2021 - 31.08.2022')
  await t
    .expect(missingPlacement2.groupMissingDuration.textContent)
    .eql('01.05.2021 - 31.08.2022')
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

test('Units list hides closed units unless toggled to show', async (t) => {
  const area = await Fixture.careArea().save()
  const closedUnit = await Fixture.daycare()
    .careArea(area)
    .with({
      name: 'Wanha päiväkoti',
      openingDate: '1900-01-01',
      closingDate: '2000-01-01'
    })
    .save()
  await t.eval(() => location.reload())

  await unitsPage.filterByName(closedUnit.data.name)
  await unitsPage.showClosedUnits(false)
  await t.expect(unitsPage.unitRows.count).eql(0)

  await unitsPage.showClosedUnits(true)
  await t.expect(unitsPage.unitRows.count).eql(1)
  await t.expect(unitsPage.unitRows.textContent).contains(closedUnit.data.name)
})
