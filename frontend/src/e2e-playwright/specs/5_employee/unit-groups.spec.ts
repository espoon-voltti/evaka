// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares,
  terminatePlacement
} from 'e2e-test-common/dev-api'
import UnitsPage from 'e2e-playwright/pages/employee/units/units'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  GroupsSection,
  UnitPage
} from 'e2e-playwright/pages/employee/units/unit'
import {
  createDaycarePlacementFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { Page } from '../../utils/page'
import {
  Child,
  Daycare,
  DaycarePlacement
} from '../../../e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
const groupId: UUID = uuidv4()
let child1Fixture: Child
let child2Fixture: Child
let child1DaycarePlacementFixture: DaycarePlacement
let child2DaycarePlacementFixture: DaycarePlacement

let daycare: Daycare
const employeeId: UUID = uuidv4()
const placementStartDate = LocalDate.today().subWeeks(4)
const placementEndDate = LocalDate.today().addWeeks(4)

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  daycare = fixtures.daycareFixture

  await insertDefaultServiceNeedOptions()

  await insertEmployeeFixture({
    id: employeeId,
    externalId: `espoo-ad:${config.unitSupervisorAad}`,
    email: 'teppo.testaaja@example.com',
    firstName: 'Teppo',
    lastName: 'Testaaja',
    roles: []
  })
  await Fixture.daycareGroup()
    .with({
      id: groupId,
      daycareId: daycare.id,
      name: 'Testailijat'
    })
    .save()
  await setAclForDaycares(`espoo-ad:${config.unitSupervisorAad}`, daycare.id)

  child1Fixture = fixtures.familyWithTwoGuardians.children[0]
  child1DaycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child1Fixture.id,
    daycare.id,
    placementStartDate.format('yyyy-MM-dd'),
    placementEndDate.format('yyyy-MM-dd')
  )

  child2Fixture = fixtures.enduserChildFixtureJari
  child2DaycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child2Fixture.id,
    daycare.id,
    placementStartDate.format('yyyy-MM-dd'),
    placementEndDate.format('yyyy-MM-dd')
  )

  await insertDaycarePlacementFixtures([
    child1DaycarePlacementFixture,
    child2DaycarePlacementFixture
  ])

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
    await insertDaycareGroupPlacementFixtures([
      {
        id: uuidv4(),
        daycareGroupId: groupId,
        daycarePlacementId: child1DaycarePlacementFixture.id,
        startDate: placementStartDate.format('yyyy-MM-dd'),
        endDate: placementEndDate.format('yyyy-MM-dd')
      }
    ])

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
      child1DaycarePlacementFixture.id,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )
    const groupsSection = await reloadUnitGroupsView()
    await groupsSection.assertTerminatedPlacementRowCount(1)
  })

  test('Child with a terminated placement is shown not in terminated placement list when termination is older than 2 weeks', async () => {
    await terminatePlacement(
      child1DaycarePlacementFixture.id,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )

    await terminatePlacement(
      child2DaycarePlacementFixture.id,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )

    const groupsSection = await reloadUnitGroupsView()
    await groupsSection.assertTerminatedPlacementRowCount(2)

    await terminatePlacement(
      child1DaycarePlacementFixture.id,
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
})
