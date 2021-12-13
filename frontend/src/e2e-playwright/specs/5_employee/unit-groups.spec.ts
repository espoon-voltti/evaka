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
import { UnitPage } from 'e2e-playwright/pages/employee/units/unit'
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
import { format } from 'date-fns'
import LocalDate from 'lib-common/local-date'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
const groupId: UUID = uuidv4()
let childFixture: Child
let daycarePlacementFixture: DaycarePlacement
let daycare: Daycare
const employeeId: UUID = uuidv4()

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

  childFixture = fixtures.familyWithTwoGuardians.children[0]
  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childFixture.id,
    daycare.id
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])

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
  test('Child with a missing group placement is shown in missing placement list and disappears when placed to a group', async () => {
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await unitPage.openUnitInformation()
    await unitPage.openGroups()
    await groupsSection.assertMissingPlacementRowCount(1)
    await insertDaycareGroupPlacementFixtures([
      {
        id: uuidv4(),
        daycareGroupId: groupId,
        daycarePlacementId: daycarePlacementFixture.id,
        startDate: format(new Date(), 'yyyy-MM-dd'),
        endDate: format(new Date(), 'yyyy-MM-dd')
      }
    ])

    await page.reload()
    await groupsSection.assertMissingPlacementRowCount(0)
  })

  test('Child with a terminated placement is shown in terminated placement list', async () => {
    await terminatePlacement(
      daycarePlacementFixture.id,
      LocalDate.today(),
      LocalDate.today(),
      employeeId
    )
    await page.reload()
    const groupsSection = await unitPage.openGroups()
    await groupsSection.openGroupCollapsible(groupId)
    await unitPage.openUnitInformation()
    await unitPage.openGroups()
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
