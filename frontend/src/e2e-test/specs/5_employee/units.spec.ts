// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import { Daycare, PersonDetail } from '../../dev-api/types'
import {
  createDefaultServiceNeedOptions,
  resetDatabase
} from '../../generated/api-clients'
import { DevDaycareGroup, DevPlacement } from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import UnitsPage from '../../pages/employee/units/units'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let unitFixture: Daycare
let groupFixture: DevDaycareGroup
let childFixture: PersonDetail
let placementFixture: DevPlacement
let page: Page

const placementDates = () => ({
  start: placementFixture.startDate.format(),
  end: placementFixture.endDate.format()
})

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()
  await createDefaultServiceNeedOptions()
  unitFixture = fixtures.daycareFixture
  childFixture = fixtures.familyWithTwoGuardians.children[0]
  groupFixture = (
    await Fixture.daycareGroup()
      .with({
        daycareId: unitFixture.id,
        name: 'Kosmiset vakiot',
        startDate: LocalDate.of(2020, 2, 1)
      })
      .save()
  ).data

  const today = LocalDate.of(2022, 12, 1)
  placementFixture = (
    await Fixture.placement()
      .with({
        childId: childFixture.id,
        unitId: unitFixture.id,
        startDate: today,
        endDate: today.addYears(1)
      })
      .save()
  ).data

  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open({
    mockedTime: LocalDate.of(2022, 12, 1).toHelsinkiDateTime(
      LocalTime.of(12, 0)
    )
  })
  await employeeLogin(page, admin.data)
})

describe('Employee - Units', () => {
  test('filtering units, navigating to one, contact details', async () => {
    const unitsPage = await UnitsPage.open(page)

    await unitsPage.filterByName(unitFixture.name)
    await unitsPage.assertRowCount(1)
    await unitsPage.nthUnitRow(0).assertFields({
      name: unitFixture.name,
      visitingAddress: `${unitFixture.streetAddress}, ${unitFixture.postalCode}`
    })

    const unitPage = await unitsPage.nthUnitRow(0).openUnit()
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.assertUnitName(unitFixture.name)
    await unitInfoPage.assertVisitingAddress(
      `${unitFixture.streetAddress}, ${unitFixture.postalCode} ${unitFixture.postOffice}`
    )
  })

  test('daycare has an empty group', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const groupsPage = await unitPage.openGroupsPage()
    await groupsPage.assertGroupCount(1)

    const group = await groupsPage.openGroupCollapsible(groupFixture.id)
    await group.assertGroupName(groupFixture.name)
    await group.assertGroupStartDate(groupFixture.startDate.format())
    await group.assertChildCount(0)
  })

  test('Unit group name, start date and end date can all be updated', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    await groupsPage.assertGroupCount(1)
    const group = await groupsPage.openGroupCollapsible(groupFixture.id)

    await group.edit({
      name: 'Uusi nimi',
      startDate: '01.01.2020',
      endDate: '31.12.2022'
    })

    await group.assertGroupName('Uusi nimi')
    await group.assertGroupStartDate('01.01.2020')
    await group.assertGroupEndDate('31.12.2022')
  })

  test('daycare has one child missing group', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    await groupsPage.missingPlacementsSection.assertRowCount(1)
    await groupsPage.missingPlacementsSection.assertRowFields(0, {
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      dateOfBirth: childFixture.dateOfBirth.format(),
      placementDuration: `${placementDates().start} - ${placementDates().end}`,
      groupMissingDuration: `${placementDates().start} - ${
        placementDates().end
      }`
    })
  })

  test('child can be placed into a group and removed from it', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    // open the group placement modal and submit it with default values
    await groupsPage.missingPlacementsSection.createGroupPlacementForChild(0)
    await groupsPage.waitUntilLoaded()

    // no more missing placements
    await groupsPage.missingPlacementsSection.assertRowCount(0)

    // check child is listed in group
    const group = await groupsPage.openGroupCollapsible(groupFixture.id)
    await group.assertChildCount(1)
    const childRow = group.childRow(childFixture.id)
    await childRow.assertFields({
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      placementDuration: `${placementDates().start}- ${placementDates().end}`
    })

    // after removing the child is again visible at missing groups and no longer at the group
    await childRow.remove()
    await groupsPage.missingPlacementsSection.assertRowCount(1)
    await groupsPage.missingPlacementsSection.assertRowFields(0, {
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      dateOfBirth: childFixture.dateOfBirth.format(),
      placementDuration: `${placementDates().start} - ${placementDates().end}`,
      groupMissingDuration: `${placementDates().start} - ${
        placementDates().end
      }`
    })

    await group.assertChildCount(0)
  })

  test('Unit occupancy rates cannot be determined when no caretaker', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const unitAttendancePage = await unitPage.openWeekCalendar()
    await unitAttendancePage.occupancies.assertNoValidValues()
  })

  test('Unit occupancy rates are correct with properly set caretaker counts', async () => {
    await Fixture.daycareCaretakers()
      .with({
        groupId: groupFixture.id,
        amount: 1,
        startDate: groupFixture.startDate
      })
      .save()

    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const unitAttendancePage = await unitPage.openWeekCalendar()
    await unitAttendancePage.selectPeriod('3 months')
    await unitAttendancePage.occupancies.assertConfirmed(
      'Min. 14,3 %',
      'Max. 14,3 %'
    )
    await unitAttendancePage.occupancies.assertPlanned(
      'Min. 14,3 %',
      'Max. 14,3 %'
    )
  })

  test('Units list hides closed units unless toggled to show', async () => {
    const area = await Fixture.careArea().save()
    const closedUnit = await Fixture.daycare()
      .careArea(area)
      .with({
        name: 'Wanha päiväkoti',
        openingDate: LocalDate.of(1900, 1, 1),
        closingDate: LocalDate.of(2000, 1, 1)
      })
      .save()

    const unitsPage = await UnitsPage.open(page)
    await unitsPage.filterByName(closedUnit.data.name)
    await unitsPage.showClosedUnits(false)
    await unitsPage.assertRowCount(0)

    await unitsPage.showClosedUnits(true)
    await unitsPage.assertRowCount(1)
    await unitsPage
      .unitRow(closedUnit.data.id)
      .assertFields({ name: closedUnit.data.name })
  })
})
