// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import {
  Daycare,
  DaycareGroup,
  DaycarePlacement,
  PersonDetail
} from '../../dev-api/types'
import { UnitPage } from '../../pages/employee/units/unit'
import UnitsPage from '../../pages/employee/units/units'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let unitFixture: Daycare
let groupFixture: DaycareGroup
let childFixture: PersonDetail
let placementFixture: DaycarePlacement
let page: Page

const isoToFi = (isoDate: string) => LocalDate.parseIso(isoDate).format()

const placementDates = () => ({
  start: isoToFi(placementFixture.startDate),
  end: isoToFi(placementFixture.endDate)
})

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  unitFixture = fixtures.daycareFixture
  childFixture = fixtures.familyWithTwoGuardians.children[0]
  groupFixture = (
    await Fixture.daycareGroup()
      .with({
        daycareId: unitFixture.id,
        name: 'Kosmiset vakiot',
        startDate: '2020-02-01'
      })
      .save()
  ).data
  placementFixture = (
    await Fixture.placement()
      .with({ childId: childFixture.id, unitId: unitFixture.id })
      .save()
  ).data

  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
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
    await unitPage.assertUnitName(unitFixture.name)
    await unitPage.assertVisitingAddress(
      `${unitFixture.streetAddress}, ${unitFixture.postalCode} ${unitFixture.postOffice}`
    )
  })

  test('daycare has an empty group', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    const groupsPage = await unitPage.openGroupsPage()
    await groupsPage.assertGroupCount(1)

    const group = await groupsPage.openGroupCollapsible(groupFixture.id)
    await group.assertGroupName(groupFixture.name)
    await group.assertGroupStartDate(
      LocalDate.parseIso(groupFixture.startDate).format()
    )
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
      dateOfBirth: isoToFi(childFixture.dateOfBirth),
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
      dateOfBirth: isoToFi(childFixture.dateOfBirth),
      placementDuration: `${placementDates().start} - ${placementDates().end}`,
      groupMissingDuration: `${placementDates().start} - ${
        placementDates().end
      }`
    })

    await group.assertChildCount(0)
  })

  test('Unit occupancy rates cannot be determined when no caretaker', async () => {
    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    await unitPage.occupancies.assertNoValidValues()
  })

  test('Unit occupancy rates are correct with properly set caretaker counts', async () => {
    await Fixture.daycareCaretakers()
      .with({
        groupId: groupFixture.id,
        amount: 1,
        startDate: LocalDate.parseIso(groupFixture.startDate)
      })
      .save()

    const unitPage = await UnitPage.openUnit(page, unitFixture.id)
    await unitPage.occupancies.assertConfirmed('Min. 14,3 %', 'Max. 14,3 %')
    await unitPage.occupancies.assertPlanned('Min. 14,3 %', 'Max. 14,3 %')
  })

  test('Units list hides closed units unless toggled to show', async () => {
    const area = await Fixture.careArea().save()
    const closedUnit = await Fixture.daycare()
      .careArea(area)
      .with({
        name: 'Wanha päiväkoti',
        openingDate: '1900-01-01',
        closingDate: '2000-01-01'
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
