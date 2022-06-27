// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addDays, subDays } from 'date-fns'

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import ChildInformationPage, {
  AssistanceNeedSection
} from '../../pages/employee/child-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let assistanceNeeds: AssistanceNeedSection
let childId: UUID
let unitId: UUID
let admin: EmployeeDetail

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  page = await Page.open()
  admin = (await Fixture.employeeAdmin().save()).data
})

const setupPlacement = async (childPlacementType: PlacementType) => {
  await insertDaycarePlacementFixtures([
    createDaycarePlacementFixture(
      uuidv4(),
      childId,
      unitId,
      LocalDate.todayInSystemTz().formatIso(),
      LocalDate.todayInSystemTz().formatIso(),
      childPlacementType
    )
  ])
}

const logUserIn = async (user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  assistanceNeeds = await childInformationPage.openCollapsible('assistanceNeed')
}

describe('Child Information assistance need functionality for employees', () => {
  test('assistance need can be added', async () => {
    await setupPlacement('DAYCARE')
    await logUserIn((await Fixture.employeeUnitSupervisor(unitId).save()).data)
    await assistanceNeeds.createNewAssistanceNeed()
    await assistanceNeeds.setAssistanceNeedMultiplier('1,5')
    await assistanceNeeds.confirmAssistanceNeed()
    await assistanceNeeds.assertAssistanceNeedMultiplier('1,5')
  })

  test('assistance need before preschool for a child in preschool is not shown for unit supervisor', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save())
      .data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        endDate: new Date(),
        description:
          'Test service need to be hidden because it starts before preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: addDays(new Date(), 1),
        endDate: addDays(new Date(), 1),
        description:
          'Test service need to be shown because it starts after preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await logUserIn(unitSupervisor)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need for preschool for a child in preschool is shown for unit manager', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save())
      .data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: new Date(),
        description:
          'Test service need to be shown because it starts when preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await logUserIn(unitSupervisor)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for admin', async () => {
    await setupPlacement('PRESCHOOL')
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        description: 'Test service need to be shown because user is admin',
        updatedBy: admin.id
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for Special Education Teacher', async () => {
    await setupPlacement('PRESCHOOL')
    const specialEducationTeacher = (
      await Fixture.employeeSpecialEducationTeacher(unitId).save()
    ).data

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        endDate: new Date(),
        description:
          'Test service need to be shown to SEO if she has acl rights to child',
        updatedBy: specialEducationTeacher.id
      })
      .save()

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: addDays(new Date(), 1),
        endDate: addDays(new Date(), 2),
        description:
          'Test service to be shown because it is active only during preschool period',
        updatedBy: specialEducationTeacher.id
      })
      .save()

    await logUserIn(specialEducationTeacher)
    await assistanceNeeds.assertAssistanceNeedCount(2)
  })
})

describe('Child assistance need decisions for employees', () => {
  test('Shows an empty draft in the list', async () => {
    await Fixture.assistanceNeedDecision().withChild(childId).save()

    await logUserIn(admin)
    await assistanceNeeds.waitUntilAssistanceNeedDecisionsLoaded()

    const decision = assistanceNeeds.assistanceNeedDecisions(0)

    await waitUntilEqual(() => decision.date, '-')
    await waitUntilEqual(() => decision.unitName, '-')
    await waitUntilEqual(() => decision.sentDate, '-')
    await waitUntilEqual(() => decision.decisionMadeDate, '-')
    await waitUntilEqual(() => decision.status, 'DRAFT')
    await waitUntilEqual(() => decision.actionCount, 2)
  })

  test('Shows a filled in draft in the list', async () => {
    const serviceWorker = (await Fixture.employeeServiceWorker().save()).data
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        selectedUnit: { id: daycareFixture.id },
        decisionMaker: {
          employeeId: serviceWorker.id,
          title: 'head teacher'
        },
        preparedBy1: {
          employeeId: serviceWorker.id,
          title: 'teacher',
          phoneNumber: '010202020202'
        },
        guardianInfo: [
          {
            id: null,
            personId: familyWithTwoGuardians.guardian.id,
            isHeard: true,
            name: '',
            details: 'Guardian 1 details'
          }
        ],
        sentForDecision: LocalDate.of(2020, 5, 11),
        startDate: LocalDate.of(2020, 7, 1),
        endDate: LocalDate.of(2020, 12, 11),
        decisionMade: LocalDate.of(2020, 6, 2)
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.waitUntilAssistanceNeedDecisionsLoaded()

    const decision = assistanceNeeds.assistanceNeedDecisions(0)

    await waitUntilEqual(() => decision.date, '01.07.2020 – 11.12.2020')
    await waitUntilEqual(() => decision.unitName, daycareFixture.name)
    await waitUntilEqual(() => decision.sentDate, '11.05.2020')
    await waitUntilEqual(() => decision.decisionMadeDate, '02.06.2020')
    await waitUntilEqual(() => decision.status, 'DRAFT')
    await waitUntilEqual(() => decision.actionCount, 2)
  })

  test('Hides edit and delete actions for non-draft/non-workable decisions', async () => {
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        status: 'ACCEPTED'
      })
      .save()
    await Fixture.preFilledAssistanceNeedDecision()
      .withChild(childId)
      .with({
        status: 'REJECTED'
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.waitUntilAssistanceNeedDecisionsLoaded()

    const acceptedDecision = assistanceNeeds.assistanceNeedDecisions(0)
    await waitUntilEqual(() => acceptedDecision.status, 'ACCEPTED')
    await waitUntilEqual(() => acceptedDecision.actionCount, 0)

    const rejectedDecision = assistanceNeeds.assistanceNeedDecisions(1)
    await waitUntilEqual(() => rejectedDecision.status, 'REJECTED')
    await waitUntilEqual(() => rejectedDecision.actionCount, 0)
  })
})
