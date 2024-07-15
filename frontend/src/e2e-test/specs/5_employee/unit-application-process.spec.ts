// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import {
  applicationFixture,
  testDaycare,
  testChild,
  testChild2,
  testAdult,
  Fixture,
  uuidv4,
  familyWithTwoGuardians,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import {
  DevApplicationWithForm,
  DevDaycare,
  DevEmployee,
  DevPerson
} from '../../generated/api-types'
import {
  ApplicationProcessPage,
  UnitPage
} from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
const groupId: UUID = uuidv4()
let child1Fixture: DevPerson
let child2Fixture: DevPerson
let child1DaycarePlacementId: UUID
let child2DaycarePlacementId: UUID

let daycare: DevDaycare
let unitSupervisor: DevEmployee
const placementStartDate = LocalDate.todayInSystemTz().subWeeks(4)
const placementEndDate = LocalDate.todayInSystemTz().addWeeks(4)

beforeEach(async () => {
  await resetServiceState()

  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family(familyWithTwoGuardians).save()
  daycare = testDaycare

  unitSupervisor = await Fixture.employee().unitSupervisor(daycare.id).save()

  await createDefaultServiceNeedOptions()

  await Fixture.daycareGroup({
    id: groupId,
    daycareId: daycare.id,
    name: 'Testailijat'
  }).save()

  child1Fixture = familyWithTwoGuardians.children[0]
  child1DaycarePlacementId = uuidv4()
  await Fixture.placement({
    id: child1DaycarePlacementId,
    childId: child1Fixture.id,
    unitId: daycare.id,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  child2Fixture = testChild2
  child2DaycarePlacementId = uuidv4()
  await Fixture.placement({
    id: child2DaycarePlacementId,
    childId: child2Fixture.id,
    unitId: daycare.id,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()
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
    const today = LocalDate.todayInSystemTz()

    const application1: DevApplicationWithForm = {
      ...applicationFixture(testChild, testAdult),
      id: uuidv4(),
      status: 'WAITING_UNIT_CONFIRMATION'
    }
    const application2: DevApplicationWithForm = {
      ...applicationFixture(testChild2, testAdult),
      id: uuidv4(),
      status: 'WAITING_UNIT_CONFIRMATION'
    }

    await createApplications({ body: [application1, application2] })

    await Fixture.placementPlan({
      applicationId: application1.id,
      unitId: testDaycare.id,
      periodStart: today,
      periodEnd: today
    }).save()

    await Fixture.placementPlan({
      applicationId: application2.id,
      unitId: testDaycare.id,
      periodStart: today,
      periodEnd: today
    }).save()

    await Fixture.decision({
      applicationId: application2.id,
      employeeId: unitSupervisor.id,
      unitId: testDaycare.id,
      startDate: today,
      endDate: today
    }).save()

    // The second decision is used to ensure that multiple decisions do not create multiple identical proposals (a bug)
    await Fixture.decision({
      applicationId: application2.id,
      employeeId: unitSupervisor.id,
      unitId: testDaycare.id,
      startDate: today.addDays(1),
      endDate: today.addDays(2)
    }).save()

    const applicationProcessPage = await loadUnitApplicationProcessPage()
    await applicationProcessPage.placementProposals.assertPlacementProposalRowCount(
      2
    )
  })
})
