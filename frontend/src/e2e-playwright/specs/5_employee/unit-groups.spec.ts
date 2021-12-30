// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import config from 'e2e-test-common/config'
import {
  insertDefaultServiceNeedOptions,
  resetDatabase,
  terminatePlacement
} from 'e2e-test-common/dev-api'
import UnitsPage from 'e2e-playwright/pages/employee/units/units'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  GroupsSection,
  UnitPage
} from 'e2e-playwright/pages/employee/units/unit'
import { Fixture, uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { Page } from '../../utils/page'
import { Child, Daycare } from '../../../e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
const groupId: UUID = uuidv4()
let child1Fixture: Child
let child2Fixture: Child
let child1DaycarePlacementId: UUID
let child2DaycarePlacementId: UUID

let daycare: Daycare
const employeeId: UUID = config.unitSupervisorAad
const placementStartDate = LocalDate.today().subWeeks(4)
const placementEndDate = LocalDate.today().addWeeks(4)

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  daycare = fixtures.daycareFixture

  await insertDefaultServiceNeedOptions()

  await Fixture.employee()
    .with({
      id: config.unitSupervisorAad,
      externalId: `espoo-ad:${config.unitSupervisorAad}`,
      roles: []
    })
    .withDaycareAcl(fixtures.daycareFixture.id, 'UNIT_SUPERVISOR')
    .save()
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

  page = await Page.open()
  await employeeLogin(page, 'UNIT_SUPERVISOR')
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  await nav.openTab('units')
  const units = new UnitsPage(page)
  await units.navigateToUnit(fixtures.daycareFixture.id)
  unitPage = new UnitPage(page)
})

describe('Employee - unit view', () => {
  test('Children with a missing group placement is shown in missing placement list and disappears when placed to a group', async () => {
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await unitPage.openUnitInformation()
    await unitPage.openGroups()
    await groupsSection.assertMissingGroupPlacementRowCount(2)

    await Fixture.groupPlacement()
      .with({
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementId,
        startDate: placementStartDate.format('yyyy-MM-dd'),
        endDate: placementEndDate.format('yyyy-MM-dd')
      })
      .save()

    await page.reload()
    await groupsSection.assertMissingGroupPlacementRowCount(1)
  })

  const reloadUnitGroupsView = async (): Promise<GroupsSection> => {
    await page.reload()
    await unitPage.openUnitInformation()
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    return groupsSection
  }

  test('Child with a terminated placement is shown in terminated placement list', async () => {
    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )
    const groupsSection = await reloadUnitGroupsView()
    await groupsSection.assertTerminatedPlacementRowCount(1)
  })

  test('Child with a terminated placement is shown not in terminated placement list when termination is older than 2 weeks', async () => {
    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )

    await terminatePlacement(
      child2DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )

    const groupsSection = await reloadUnitGroupsView()
    await groupsSection.assertTerminatedPlacementRowCount(2)

    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today().subDays(15),
      employeeId
    )
    await reloadUnitGroupsView()
    await groupsSection.assertTerminatedPlacementRowCount(1)
  })

  test('Open groups stay open when reloading page', async () => {
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await page.reload()
    await groupsSection.assertGroupCollapsibleIsOpen(groupId)
  })

  test('Open groups stay open when visiting other unit page tab', async () => {
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await unitPage.openUnitInformation()
    await unitPage.openGroups()
    await groupsSection.assertGroupCollapsibleIsOpen(groupId)
  })

  test('Staff will not see terminated placements', async () => {
    await Fixture.employee()
      .with({
        id: config.staffAad,
        externalId: `espoo-ad:${config.staffAad}`,
        roles: []
      })
      .withDaycareAcl(daycare.id, 'STAFF')
      .save()

    await terminatePlacement(
      child1DaycarePlacementId,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )

    await employeeLogin(page, 'STAFF')
    await page.goto(`${config.employeeUrl}/units/${daycare.id}`)
    const groupsSection = await reloadUnitGroupsView()
    await groupsSection.assertTerminatedPlacementRowCount(0)
  })

  test('Staff will not see children with a missing group placement', async () => {
    await Fixture.employee()
      .with({
        id: config.staffAad,
        externalId: `espoo-ad:${config.staffAad}`,
        roles: []
      })
      .withDaycareAcl(daycare.id, 'STAFF')
      .save()

    await employeeLogin(page, 'STAFF')
    await page.goto(`${config.employeeUrl}/units/${daycare.id}`)
    const groupsSection = await reloadUnitGroupsView()
    await groupsSection.assertMissingGroupPlacementRowCount(0)
  })
})
