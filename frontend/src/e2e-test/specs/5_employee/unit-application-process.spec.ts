// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ApplicationId,
  GroupId,
  PlacementId
} from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import {
  applicationFixture,
  testDaycare,
  testChild,
  testChild2,
  testAdult,
  Fixture,
  familyWithTwoGuardians,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type {
  DevApplicationWithForm,
  DevDaycare,
  DevEmployee,
  DevPerson
} from '../../generated/api-types'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import type { ApplicationProcessPage } from '../../pages/employee/units/unit'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
const groupId = randomId<GroupId>()
let child1Fixture: DevPerson
let child2Fixture: DevPerson
let child1DaycarePlacementId: PlacementId
let child2DaycarePlacementId: PlacementId

let daycare: DevDaycare
let unitSupervisor: DevEmployee
const placementStartDate = LocalDate.todayInSystemTz().subWeeks(4)
const placementEndDate = LocalDate.todayInSystemTz().addWeeks(4)

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await familyWithTwoGuardians.save()
  daycare = testDaycare

  unitSupervisor = await Fixture.employee().unitSupervisor(daycare.id).save()

  await createDefaultServiceNeedOptions()

  await Fixture.daycareGroup({
    id: groupId,
    daycareId: daycare.id,
    name: 'Testailijat'
  }).save()

  child1Fixture = familyWithTwoGuardians.children[0]
  child1DaycarePlacementId = randomId()
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
  child2DaycarePlacementId = randomId()
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
      id: randomId<ApplicationId>(),
      status: 'WAITING_UNIT_CONFIRMATION',
      confidential: true
    }
    const application2: DevApplicationWithForm = {
      ...applicationFixture(testChild2, testAdult),
      id: randomId<ApplicationId>(),
      status: 'WAITING_UNIT_CONFIRMATION',
      confidential: true
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

  test('Application is shown in the unit page until guardian handles both decisions', async () => {
    const today = LocalDate.todayInSystemTz()
    const mockChild = Fixture.person({ ssn: '010116A9219' })
    const mockAdult = Fixture.person({ ssn: '010106A973C' })
    await Fixture.preschoolTerm({
      finnishPreschool: new FiniteDateRange(
        today.subMonths(1),
        today.addMonths(1)
      ),
      swedishPreschool: new FiniteDateRange(
        today.subMonths(1),
        today.addMonths(1)
      ),
      extendedTerm: new FiniteDateRange(today.subMonths(1), today.addMonths(1)),
      applicationPeriod: new FiniteDateRange(
        today.subMonths(1),
        today.addMonths(1)
      )
    }).save()
    const testArea = await Fixture.careArea().save()
    const testDaycare = await Fixture.daycare({
      areaId: testArea.id
    }).save()
    const mockUnitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()
    await Fixture.family({
      guardian: mockAdult,
      children: [mockChild]
    }).save()
    const testApplication: DevApplicationWithForm = {
      ...applicationFixture(
        mockChild,
        mockAdult,
        undefined,
        'PRESCHOOL',
        null,
        [testDaycare.id],
        true
      ),
      id: randomId<ApplicationId>(),
      type: 'PRESCHOOL',
      status: 'WAITING_CONFIRMATION',
      confidential: true
    }
    await createApplications({ body: [testApplication] })

    await Fixture.placementPlan({
      applicationId: testApplication.id,
      unitId: testDaycare.id,
      periodStart: today,
      periodEnd: today,
      preschoolDaycarePeriodStart: today,
      preschoolDaycarePeriodEnd: today.addDays(7)
    }).save()

    const decision1 = await Fixture.decision({
      applicationId: testApplication.id,
      employeeId: mockUnitSupervisor.id,
      unitId: testDaycare.id,
      type: 'PRESCHOOL',
      status: 'PENDING',
      startDate: today,
      endDate: today
    }).save()

    const decision2 = await Fixture.decision({
      applicationId: testApplication.id,
      employeeId: mockUnitSupervisor.id,
      unitId: testDaycare.id,
      type: 'PRESCHOOL_DAYCARE',
      status: 'PENDING',
      startDate: today.addDays(1),
      endDate: today.addDays(1)
    }).save()

    // Unit supervisor opens the unit application process page and checks that
    // the application is shown
    await employeeLogin(page, mockUnitSupervisor)
    let unitPage = new UnitPage(page)
    await unitPage.navigateToUnit(testDaycare.id)
    let applicationProcessPage = await unitPage.openApplicationProcessTab()
    await applicationProcessPage.waitUntilVisible()
    await applicationProcessPage.waitingConfirmation.assertRowCount(1)
    await page.close()

    // Guardian accepts the first decision
    page = await Page.open()
    await enduserLogin(page, mockAdult)
    let header = new CitizenHeader(page)
    await header.selectTab('decisions')
    let citizenDecisionsPage = new CitizenDecisionsPage(page)
    let responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
      testApplication.id
    )
    await responsePage.acceptDecision(decision1.id)
    await page.close()

    // Unit supervisor checks that the application is still shown
    page = await Page.open()
    await employeeLogin(page, mockUnitSupervisor)
    unitPage = new UnitPage(page)
    await unitPage.navigateToUnit(testDaycare.id)
    applicationProcessPage = await unitPage.openApplicationProcessTab()
    await applicationProcessPage.waitingConfirmation.assertRowCount(1)
    await page.close()

    // Guardian accepts the second decision
    page = await Page.open()
    await enduserLogin(page, mockAdult)
    header = new CitizenHeader(page)
    await header.selectTab('decisions')
    citizenDecisionsPage = new CitizenDecisionsPage(page)
    responsePage = await citizenDecisionsPage.navigateToDecisionResponse(
      testApplication.id
    )
    await responsePage.acceptDecision(decision2.id)
    await page.close()

    // Unit supervisor checks that the application is no longer shown
    page = await Page.open()
    await employeeLogin(page, mockUnitSupervisor)
    unitPage = new UnitPage(page)
    await unitPage.navigateToUnit(testDaycare.id)
    applicationProcessPage = await unitPage.openApplicationProcessTab()
    await applicationProcessPage.waitingConfirmation.assertRowCount(0)
  })
})
