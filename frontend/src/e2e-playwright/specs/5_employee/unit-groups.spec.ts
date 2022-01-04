// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  insertDefaultServiceNeedOptions,
  resetDatabase,
  terminatePlacement
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import { UnitPage } from 'e2e-playwright/pages/employee/units/unit'
import { Fixture, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { Page } from '../../utils/page'
import {
  Child,
  Daycare,
  EmployeeDetail
} from '../../../e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'
import { UnitGroupsPage } from '../../pages/employee/units/unit-groups-page'

let page: Page
let unitPage: UnitPage
const groupId: UUID = uuidv4()
let child1Fixture: Child
let child2Fixture: Child
let child1DaycarePlacementId: UUID
let child2DaycarePlacementId: UUID

let daycare: Daycare
let unitSupervisor: EmployeeDetail
const placementStartDate = LocalDate.today().subWeeks(4)
const placementEndDate = LocalDate.today().addWeeks(4)

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  daycare = fixtures.daycareFixture

  unitSupervisor = (await Fixture.employeeUnitSupervisor(daycare.id).save())
    .data

  await insertDefaultServiceNeedOptions()

  await Fixture.daycareGroup()
    .with({
      id: groupId,
      daycareId: daycare.id,
      name: 'Testailijat'
    })
    .save()

  child1Fixture = fixtures.familyWithTwoGuardians.children[0]
  child1DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child1DaycarePlacementId,
      childId: child1Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate.format('yyyy-MM-dd'),
      endDate: placementEndDate.format('yyyy-MM-dd')
    })
    .save()

  child2Fixture = fixtures.enduserChildFixtureJari
  child2DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child2DaycarePlacementId,
      childId: child2Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate.format('yyyy-MM-dd'),
      endDate: placementEndDate.format('yyyy-MM-dd')
    })
    .save()
})

const loadUnitGroupsPage = async (): Promise<UnitGroupsPage> => {
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  const groupsPage = await unitPage.openGroupsPage()
  await groupsPage.waitUntilVisible()
  return groupsPage
}

describe('Unit groups - unit supervisor', () => {
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, unitSupervisor)
  })

  test('Children with a missing group placement is shown in missing placement list and disappears when placed to a group', async () => {
    const groupsSection = await loadUnitGroupsPage()
    await groupsSection.missingPlacementsSection.assertRowCount(2)

    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementId,
        startDate: placementStartDate.format('yyyy-MM-dd'),
        endDate: placementEndDate.format('yyyy-MM-dd')
      })
      .save()

    await page.reload()
    await groupsSection.missingPlacementsSection.assertRowCount(1)
  })

  test('Child with a terminated placement is shown in terminated placement list', async () => {
    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      unitSupervisor.id
    )
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.terminatedPlacementsSection.assertRowCount(1)
  })

  test('Child with a terminated placement is shown not in terminated placement list when termination is older than 2 weeks', async () => {
    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      unitSupervisor.id
    )

    await terminatePlacement(
      child2DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      unitSupervisor.id
    )

    let groupsPage = await loadUnitGroupsPage()
    await groupsPage.terminatedPlacementsSection.assertRowCount(2)

    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today().subDays(15),
      unitSupervisor.id
    )
    groupsPage = await loadUnitGroupsPage()
    await groupsPage.terminatedPlacementsSection.assertRowCount(1)
  })

  test('Open groups stay open when reloading page', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.openGroupCollapsible(groupId)
    await page.reload()
    await groupsPage.assertGroupCollapsibleIsOpen(groupId)
  })

  test('Open groups stay open when visiting other unit page tab', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.openGroupCollapsible(groupId)
    await unitPage.openUnitInformation()
    await unitPage.openGroupsPage()
    await groupsPage.assertGroupCollapsibleIsOpen(groupId)
  })

  test('Supervisor sees child occupancy factor', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.missingPlacementsSection.assertRowCount(2)

    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementId,
        startDate: placementStartDate.format('yyyy-MM-dd'),
        endDate: placementEndDate.format('yyyy-MM-dd')
      })
      .save()

    await page.reload()
    await groupsPage.openGroupCollapsible(groupId)
    await groupsPage.childCapacityFactorColumnHeading.waitUntilVisible()
    await groupsPage.assertChildCapacityFactor(child1Fixture.id, '1.00')
  })
})

describe('Unit groups - staff', () => {
  beforeEach(async () => {
    const staff = await Fixture.employeeStaff(daycare.id).save()

    page = await Page.open()
    await employeeLogin(page, staff.data)
  })

  test('Staff will not see terminated placements', async () => {
    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      unitSupervisor.id
    )
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.terminatedPlacementsSection.assertRowCount(0)
  })

  test('Staff will not see children with a missing group placement', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.missingPlacementsSection.assertRowCount(0)
  })

  test('Staff does not see child occupancy factor', async () => {
    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementId,
        startDate: placementStartDate.format('yyyy-MM-dd'),
        endDate: placementEndDate.format('yyyy-MM-dd')
      })
      .save()

    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.childOccupancyFactorColumnHeading.waitUntilHidden()
    await groupsPage.assertChildOccupancyFactorColumnNotVisible()
  })
})
