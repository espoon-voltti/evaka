// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  familyWithTwoGuardians,
  Fixture,
  testCareArea,
  testClub,
  testDaycare,
  testDaycarePrivateVoucher,
  testPreschool
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type {
  DevDaycare,
  DevDaycareGroup,
  DevPerson,
  DevPlacement
} from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import UnitsPage from '../../pages/employee/units/units'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let unitFixture: DevDaycare
let groupFixture: DevDaycareGroup
let childFixture: DevPerson
let placementFixture: DevPlacement
let page: Page

const placementDates = () => ({
  start: placementFixture.startDate.format(),
  end: placementFixture.endDate.format()
})

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await testDaycarePrivateVoucher.save()
  await testPreschool.save()
  await testClub.save()
  await familyWithTwoGuardians.save()
  await createDefaultServiceNeedOptions()
  unitFixture = testDaycare
  childFixture = familyWithTwoGuardians.children[0]
  groupFixture = await Fixture.daycareGroup({
    daycareId: unitFixture.id,
    name: 'Kosmiset vakiot',
    startDate: LocalDate.of(2020, 2, 1)
  }).save()

  const today = LocalDate.of(2022, 12, 1)
  placementFixture = await Fixture.placement({
    childId: childFixture.id,
    unitId: unitFixture.id,
    startDate: today,
    endDate: today.addYears(1)
  }).save()

  const admin = await Fixture.employee().admin().save()

  page = await Page.open({
    mockedTime: LocalDate.of(2022, 12, 1).toHelsinkiDateTime(
      LocalTime.of(12, 0)
    )
  })
  await employeeLogin(page, admin)
})

describe('Employee - Units', () => {
  test('filtering units, navigating to one, contact details', async () => {
    const unitsPage = await UnitsPage.open(page)

    await unitsPage.filterByName(unitFixture.name)
    await unitsPage.assertRowCount(1)
    await unitsPage.nthUnitRow(0).assertFields({
      name: unitFixture.name,
      visitingAddress: `${unitFixture.visitingAddress.streetAddress}, ${unitFixture.visitingAddress.postalCode}`
    })

    const unitPage = await unitsPage.nthUnitRow(0).openUnit()
    const unitInfoPage = await unitPage.openUnitInformation()
    await unitInfoPage.assertUnitName(unitFixture.name)
    await unitInfoPage.assertVisitingAddress(
      `${unitFixture.visitingAddress.streetAddress}, ${unitFixture.visitingAddress.postalCode} ${unitFixture.visitingAddress.postOffice}`
    )
  })

  test('filtering units by provider type', async () => {
    const unitsPage = await UnitsPage.open(page)
    await unitsPage.assertRowCount(4)
    await unitsPage.providerTypesSelect.fillAndSelectFirst('Yksityinen')
    await unitsPage.assertRowCount(0)
    await unitsPage.providerTypesSelect.fillAndSelectFirst('Kunnallinen')
    await unitsPage.assertRowCount(3)
  })

  test('filtering units by care type', async () => {
    const unitsPage = await UnitsPage.open(page)
    await unitsPage.assertRowCount(4)
    await unitsPage.careTypesSelect.fillAndSelectFirst('Esiopetus')
    await unitsPage.assertRowCount(2)
    await unitsPage.careTypesSelect.fillAndSelectFirst('Kerho')
    await unitsPage.assertRowCount(3)
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
    const unitCalendarPage = await unitPage.openCalendarPage()
    await unitCalendarPage.occupancies.assertNoValidValues()
  })

  test('Unit occupancy rates are correct with properly set caretaker counts', async () => {
    await Fixture.daycareCaretakers({
      groupId: groupFixture.id,
      amount: 1,
      startDate: groupFixture.startDate
    }).save()

    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const unitCalendarPage = await unitPage.openCalendarPage()
    await unitCalendarPage.selectPeriod('3 months')
    await unitCalendarPage.occupancies.assertConfirmed(
      'Min. 14,3 %',
      'Max. 14,3 %'
    )
    await unitCalendarPage.occupancies.assertPlanned(
      'Min. 14,3 %',
      'Max. 14,3 %'
    )
  })

  test('Units list hides closed units unless toggled to show', async () => {
    const area = await Fixture.careArea().save()
    const closedUnit = await Fixture.daycare({
      areaId: area.id,
      name: 'Wanha päiväkoti',
      openingDate: LocalDate.of(1900, 1, 1),
      closingDate: LocalDate.of(2000, 1, 1)
    }).save()

    const unitsPage = await UnitsPage.open(page)
    await unitsPage.filterByName(closedUnit.name)
    await unitsPage.showClosedUnits(false)
    await unitsPage.assertRowCount(0)

    await unitsPage.showClosedUnits(true)
    await unitsPage.assertRowCount(1)
    await unitsPage
      .unitRow(closedUnit.id)
      .assertFields({ name: closedUnit.name })
  })
})
