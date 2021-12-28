// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Home from '../../pages/employee/home'
import UnitsPage from '../../pages/employee/units/units-page'
import UnitPage, {
  daycareGroupElement,
  daycareGroupPlacementElement,
  missingPlacementElement
} from '../../pages/employee/units/unit-page'
import GroupPlacementModal from '../../pages/employee/units/group-placement-modal'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  DaycareGroupBuilder,
  Fixture,
  PlacementBuilder
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { Daycare, PersonDetail } from 'e2e-test-common/dev-api/types'
import {
  insertDefaultServiceNeedOptions,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import LocalDate from 'lib-common/local-date'

const home = new Home()
const unitsPage = new UnitsPage()
const unitPage = new UnitPage()
const groupPlacementModal = new GroupPlacementModal()

let unitFixture: Daycare
let groupFixture: DaycareGroupBuilder
let childFixture: PersonDetail
let placementFixture: PlacementBuilder

const isoToFi = (isoDate: string) => LocalDate.parseIso(isoDate).format()
const placementDates = () => ({
  start: isoToFi(placementFixture.data.startDate),
  end: isoToFi(placementFixture.data.endDate)
})

fixture('Employee - Units')
  .meta({ type: 'regression', subType: 'units' })
  .beforeEach(async (t) => {
    await resetDatabase()
    const fixtures = await initializeAreaAndPersonData()
    await insertDefaultServiceNeedOptions()
    unitFixture = fixtures.daycareFixture
    childFixture = fixtures.familyWithTwoGuardians.children[0]
    groupFixture = await Fixture.daycareGroup()
      .with({ daycareId: unitFixture.id, name: 'Kosmiset vakiot' })
      .save()
    placementFixture = await Fixture.placement()
      .with({ childId: childFixture.id, unitId: unitFixture.id })
      .save()

    await employeeLogin(t, seppoAdmin, home.homePage('admin'))
    await home.navigateToUnits()
  })
  .afterEach(logConsoleMessages)

test('filtering units, navigating to one, contact details', async (t) => {
  await unitsPage.filterByName(unitFixture.name)
  await t.expect(unitsPage.unitRows.count).eql(1)
  await unitsPage.navigateToNthUnit(0)
  await t.expect(unitPage.unitName.textContent).eql(unitFixture.name)
  await t
    .expect(unitPage.visitingAddress.textContent)
    .eql(
      `${unitFixture.streetAddress}, ${unitFixture.postalCode} ${unitFixture.postOffice}`
    )
})

test('daycare has an empty group', async (t) => {
  await unitPage.navigateHere(unitFixture.id)
  await unitPage.openTabGroups()
  await unitPage.openGroups()
  await t.expect(unitPage.groups.count).eql(1)
  const group = daycareGroupElement(unitPage.groups.nth(0))
  await t.expect(group.groupName.textContent).eql(groupFixture.data.name)
  await t
    .expect(group.groupStartDate.textContent)
    .eql(isoToFi(groupFixture.data.startDate))
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})

test('Unit group name, start date and end date can all be updated', async (t) => {
  await unitPage.navigateHere(unitFixture.id)
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
  await unitPage.navigateHere(unitFixture.id)
  await unitPage.openTabGroups()
  await t.expect(unitPage.missingPlacementRows.count).eql(1)
  const row = missingPlacementElement(unitPage.missingPlacementRows.nth(0))
  await t
    .expect(row.childName.textContent)
    .eql(`${childFixture.lastName} ${childFixture.firstName}`)
  await t
    .expect(row.childDateOfBirth.textContent)
    .eql(LocalDate.parseIso(childFixture.dateOfBirth).format())
  await t
    .expect(row.placementDuration.textContent)
    .eql(`${placementDates().start} - ${placementDates().end}`)
  await t
    .expect(row.groupMissingDuration.textContent)
    .eql(`${placementDates().start} - ${placementDates().end}`)
  await t.expect(row.addToGroupBtn.visible).ok()
})

test('child can be placed into a group and removed from it', async (t) => {
  await unitPage.navigateHere(unitFixture.id)
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
    .eql(`${childFixture.lastName} ${childFixture.firstName}`)
  await t
    .expect(groupPlacement.placementDuration.textContent)
    .eql(`${placementDates().start}- ${placementDates().end}`)

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
    .eql(`${placementDates().start} - ${placementDates().end}`)
  await t
    .expect(missingPlacement2.groupMissingDuration.textContent)
    .eql(`${placementDates().start} - ${placementDates().end}`)
  await t.expect(group.groupPlacementRows.count).eql(0)
  await t.expect(group.noChildrenPlaceholder.visible).ok()
})

test('Unit occupancy rates cannot be determined when no caretaker', async (t) => {
  await unitPage.navigateHere(unitFixture.id)

  await t.expect(unitPage.occupancies('confirmed').noValidValues.exists).ok()
  await t.expect(unitPage.occupancies('planned').noValidValues.exists).ok()
})

test('Unit occupancy rates are correct with properly set caretaker counts', async (t) => {
  await Fixture.daycareCaretakers()
    .with({
      groupId: groupFixture.data.id,
      amount: 1,
      startDate: LocalDate.parseIso(groupFixture.data.startDate)
    })
    .save()
  await unitPage.navigateHere(unitFixture.id)
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
