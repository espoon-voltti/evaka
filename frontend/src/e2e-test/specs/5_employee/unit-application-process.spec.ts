// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import {
  insertApplications,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  applicationFixture,
  daycareFixture,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  Application,
  Child,
  Daycare,
  EmployeeDetail
} from '../../dev-api/types'
import {
  ApplicationProcessPage,
  UnitPage
} from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

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

const loadUnitApplicationProcessPage =
  async (): Promise<ApplicationProcessPage> => {
    unitPage = new UnitPage(page)
    await unitPage.navigateToUnit(daycare.id)
    const ApplicationProcessPage = await unitPage.openApplicationProcessTab()
    await ApplicationProcessPage.waitUntilVisible()
    return ApplicationProcessPage
  }

describe('Unit groups - placement plans / proposals', () => {
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, unitSupervisor)
  })

  test('Placement plan is shown', async () => {
    const today = LocalDate.today()

    //const groupsSection = await loadUnitGroupsPage()
    //await groupsSection.missingPlacementsSection.assertRowCount(2)
    const application1: Application = {
      ...applicationFixture(enduserChildFixtureJari, enduserGuardianFixture),
      id: uuidv4(),
      status: 'WAITING_UNIT_CONFIRMATION'
    }
    const application2: Application = {
      ...applicationFixture(enduserChildFixtureKaarina, enduserGuardianFixture),
      id: uuidv4(),
      status: 'WAITING_UNIT_CONFIRMATION'
    }

    await insertApplications([application1, application2])

    await Fixture.placementPlan()
      .with({
        applicationId: application1.id,
        unitId: daycareFixture.id,
        periodStart: today.format('yyyy-MM-dd'),
        periodEnd: today.format('yyyy-MM-dd')
      })
      .save()

    await Fixture.placementPlan()
      .with({
        applicationId: application2.id,
        unitId: daycareFixture.id,
        periodStart: today.format('yyyy-MM-dd'),
        periodEnd: today.format('yyyy-MM-dd')
      })
      .save()

    await Fixture.decision()
      .with({
        applicationId: application2.id,
        employeeId: unitSupervisor.id,
        unitId: daycareFixture.id,
        startDate: today.format('yyyy-MM-dd'),
        endDate: today.format('yyyy-MM-dd')
      })
      .save()

    // The second decision is used to ensure that multiple decisions do not create multiple identical proposals (a bug)
    await Fixture.decision()
      .with({
        applicationId: application2.id,
        employeeId: unitSupervisor.id,
        unitId: daycareFixture.id,
        startDate: today.addDays(1).format('yyyy-MM-dd'),
        endDate: today.addDays(2).format('yyyy-MM-dd')
      })
      .save()

    const applicationProcessPage = await loadUnitApplicationProcessPage()
    await applicationProcessPage.placementProposals.assertPlacementProposalRowCount(
      2
    )
  })
})
