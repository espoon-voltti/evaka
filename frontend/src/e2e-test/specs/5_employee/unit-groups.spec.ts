// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDefaultServiceNeedOptions,
  resetDatabase,
  terminatePlacement
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { Fixture, uuidv4 } from '../../dev-api/fixtures'
import { Child, Daycare, EmployeeDetail } from '../../dev-api/types'
import ChildInformationPage, {
  AssistanceNeedSection
} from '../../pages/employee/child-information'
import { UnitPage } from '../../pages/employee/units/unit'
import { UnitGroupsPage } from '../../pages/employee/units/unit-groups-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let childInformationPage: ChildInformationPage
let assistanceNeeds: AssistanceNeedSection
const groupId: UUID = uuidv4()
let child1Fixture: Child
let child2Fixture: Child
// let child3Fixture: Child
let child1DaycarePlacementId: UUID
let child2DaycarePlacementId: UUID
// let child3DaycarePlacementId: UUID

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
      startDate: placementStartDate.formatIso(),
      endDate: placementEndDate.formatIso()
    })
    .save()

  child2Fixture = fixtures.personFixtureChildZeroYearOld
  child2DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child2DaycarePlacementId,
      childId: child2Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate.formatIso(),
      endDate: placementEndDate.formatIso()
    })
    .save()

  // child3Fixture = fixtures.personFixtureChildZeroYearOld
  // child3DaycarePlacementId = uuidv4()
  // await Fixture.placement()
  //   .with({
  //     id: child3DaycarePlacementId,
  //     childId: child3Fixture.id,
  //     unitId: daycare.id,
  //     startDate: placementStartDate.formatIso(),
  //     endDate: placementEndDate.formatIso()
  //   })
  //   .save()
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
        startDate: placementStartDate.formatIso(),
        endDate: placementEndDate.formatIso()
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

  test('Supervisor sees child occupancy factor 1 as —', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.missingPlacementsSection.assertRowCount(2)

    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementId,
        startDate: placementStartDate.formatIso(),
        endDate: placementEndDate.formatIso()
      })
      .save()

    await page.reload()
    await groupsPage.openGroupCollapsible(groupId)
    await groupsPage.childCapacityFactorColumnHeading.waitUntilVisible()
    await groupsPage.assertChildCapacityFactor(child1Fixture.id, '—')
  })

  test('Supervisor sees numeric child occupancy factor when not equal to 1', async () => {
    await page.goto(
      config.employeeUrl + '/child-information/' + child1Fixture.id
    )
    childInformationPage = new ChildInformationPage(page)
    assistanceNeeds = await childInformationPage.openCollapsible(
      'assistanceNeed'
    )
    await assistanceNeeds.createNewAssistanceNeed()
    await assistanceNeeds.setAssistanceNeedMultiplier('1,5')
    await assistanceNeeds.confirmAssistanceNeed()

    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.missingPlacementsSection.assertRowCount(2)

    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementId,
        startDate: placementStartDate.formatIso(),
        endDate: placementEndDate.formatIso()
      })
      .save()

    await page.reload()
    await groupsPage.openGroupCollapsible(groupId)
    await groupsPage.childCapacityFactorColumnHeading.waitUntilVisible()
    await groupsPage.assertChildCapacityFactor(child1Fixture.id, '1.50')
  })

  test('Supervisor sees uncommon age factor when set', async () => {
    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.missingPlacementsSection.assertRowCount(2)

    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child2DaycarePlacementId,
        startDate: placementStartDate.formatIso(),
        endDate: placementEndDate.formatIso()
      })
      .save()

    const specialServiceNeed = await Fixture.serviceNeedOption()
      .with({ occupancyCoefficientUnder3y: 1.89 })
      .save()

    await Fixture.serviceNeed()
      .with({
        placementId: child2DaycarePlacementId,
        optionId: specialServiceNeed.data.id,
        confirmedBy: unitSupervisor.id
      })
      .save()

    await page.reload()
    await groupsPage.openGroupCollapsible(groupId)
    await groupsPage.childCapacityFactorColumnHeading.waitUntilVisible()
    await groupsPage.assertChildCapacityFactor(child2Fixture.id, '1.89')
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
        startDate: placementStartDate.formatIso(),
        endDate: placementEndDate.formatIso()
      })
      .save()

    const groupsPage = await loadUnitGroupsPage()
    await groupsPage.childCapacityFactorColumnHeading.waitUntilHidden()
    await groupsPage.assertChildOccupancyFactorColumnNotVisible()
  })
})
