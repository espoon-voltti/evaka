// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  AssistanceNeedDecisionId,
  DaycareId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  testCareArea2,
  testCareArea,
  createDaycarePlacementFixture,
  testDaycare2,
  testDaycare,
  testDaycareGroup,
  testChild,
  familyWithTwoGuardians,
  Fixture
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import {
  AssistanceNeedDecisionsReport,
  AssistanceNeedDecisionsReportDecision
} from '../../pages/employee/reports'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let decisionMaker: DevEmployee
let director: DevEmployee

let unitId: DaycareId
let childId: PersonId

const mockedTime = LocalDate.of(2021, 8, 16)

beforeEach(async () => {
  await resetServiceState()

  decisionMaker = await Fixture.employee().director().save()
  director = await Fixture.employee().director().save()

  await testCareArea.save()
  await testDaycare.save()
  await familyWithTwoGuardians.save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    randomId(),
    childId,
    unitId
  )

  await createDaycarePlacements({ body: [daycarePlacementFixture] })
})

describe('Assistance need decisions report', () => {
  beforeEach(async () => {
    page = await Page.open({
      mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
  })

  const baseReportRow = {
    childName: `${familyWithTwoGuardians.children[0].lastName} ${familyWithTwoGuardians.children[0].firstName}`,
    careAreaName: testCareArea.name,
    unitName: testDaycare.name,
    decisionMade: '-',
    status: 'DRAFT'
  }

  test('Lists correct decisions', async () => {
    await Fixture.assistanceNeedDecision({
      id: randomId<AssistanceNeedDecisionId>(),
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2021, 1, 6),
      selectedUnit: unitId
    }).save()
    await Fixture.assistanceNeedDecision({
      id: randomId<AssistanceNeedDecisionId>(),
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2020, 6, 8),
      selectedUnit: unitId
    }).save()
    // This one should not be visible to 'regional director', as it's assigned to a different decision-maker
    await Fixture.assistanceNeedDecision({
      childId,
      decisionMaker: {
        employeeId: director.id,
        title: 'director of another region',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2020, 1, 1),
      selectedUnit: unitId
    }).save()
    await Fixture.assistanceNeedDecision({
      id: randomId<AssistanceNeedDecisionId>(),
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2019, 9, 6),
      selectedUnit: unitId,
      status: 'ACCEPTED'
    }).save()

    await employeeLogin(page, decisionMaker)
    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)
    const report = new AssistanceNeedDecisionsReport(page)

    await report.rows.assertCount(3)
    await waitUntilEqual(() => report.row(0), {
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: true
    })
    await waitUntilEqual(() => report.row(1), {
      ...baseReportRow,
      sentForDecision: '08.06.2020',
      isUnopened: true
    })
    await waitUntilEqual(() => report.row(2), {
      ...baseReportRow,
      status: 'ACCEPTED',
      sentForDecision: '06.09.2019',
      isUnopened: true
    })
  })

  test('Removes unopened indicator from decision after opening decision', async () => {
    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2021, 1, 6),
      selectedUnit: unitId
    }).save()

    await employeeLogin(page, decisionMaker)
    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)
    await waitUntilEqual(() => new AssistanceNeedDecisionsReport(page).row(0), {
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: true
    })

    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)
    await decisionPage.decisionMaker.assertTextEquals(
      `${decisionMaker.firstName} ${decisionMaker.lastName}, regional director`
    )

    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)

    await waitUntilEqual(() => new AssistanceNeedDecisionsReport(page).row(0), {
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: false
    })
  })

  test('Returns decision for editing', async () => {
    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2021, 1, 6),
      selectedUnit: unitId
    }).save()

    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.returnForEditBtn.click()
    await decisionPage.returnForEditModal.okBtn.click()

    await decisionPage.decisionStatus.assertStatus('NEEDS_WORK')
  })

  test('Decision can be rejected', async () => {
    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2020, 6, 8),
      selectedUnit: unitId
    }).save()
    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.rejectBtn.click()
    await decisionPage.modalOkBtn.click()

    await decisionPage.decisionStatus.assertStatus('REJECTED')
  })

  test('Decision can be accepted', async () => {
    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2021, 1, 6),
      selectedUnit: unitId
    }).save()
    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.approveBtn.click()
    await decisionPage.modalOkBtn.click()

    await decisionPage.decisionStatus.assertStatus('ACCEPTED')
  })

  test('Accepted decision can be annulled', async () => {
    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2019, 9, 6),
      selectedUnit: unitId,
      status: 'ACCEPTED'
    }).save()
    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.annulBtn.click()
    await decisionPage.annulReasonInput.type('foo bar baz')
    await decisionPage.modalOkBtn.click()

    await decisionPage.decisionStatus.assertStatus('ANNULLED')
    await decisionPage.annulmentReason.assertTextEquals('foo bar baz')
  })

  test('Decision-maker can be changed', async () => {
    const admin = await Fixture.employee({
      firstName: 'Sari',
      lastName: 'Sorsa'
    })
      .admin()
      .save()

    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2019, 9, 6),
      selectedUnit: unitId
    }).save()

    await employeeLogin(page, admin)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.decisionMaker.assertTextEquals(
      `${decisionMaker.firstName} ${decisionMaker.lastName}, regional director`
    )

    await decisionPage.approveBtn.waitUntilHidden()

    await decisionPage.mismatchModalLink.click()
    await decisionPage.mismatchModal.titleInput.type('director')
    await decisionPage.mismatchModal.okBtn.click()

    await decisionPage.decisionMaker.assertTextEquals(
      `${admin.firstName} ${admin.lastName}, director`
    )

    await decisionPage.approveBtn.assertDisabled(false)
  })

  test('Special education teacher on child s unit can open the decision', async () => {
    const decisionId = randomId<AssistanceNeedDecisionId>()
    await Fixture.assistanceNeedDecision({
      id: decisionId,
      childId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2021, 1, 6),
      selectedUnit: unitId
    }).save()

    await testChild.saveChild()
    const anotherChildId = testChild.id
    const careArea = await testCareArea2.save()
    const anotherDaycare = await Fixture.daycare({
      ...testDaycare2,
      areaId: careArea.id
    }).save()

    const daycarePlacementFixture2 = createDaycarePlacementFixture(
      randomId(),
      anotherChildId,
      anotherDaycare.id
    )

    await createDaycarePlacements({ body: [daycarePlacementFixture2] })

    await Fixture.assistanceNeedDecision({
      id: randomId<AssistanceNeedDecisionId>(),
      childId: anotherChildId,
      decisionMaker: {
        employeeId: decisionMaker.id,
        title: 'regional director',
        name: null,
        phoneNumber: null
      },
      sentForDecision: LocalDate.of(2021, 1, 6),
      selectedUnit: anotherDaycare.id
    }).save()

    const VEO = await Fixture.employee().specialEducationTeacher(unitId).save()

    await employeeLogin(page, VEO)
    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)

    const assistanceNeedDecisionsPage = new AssistanceNeedDecisionsReport(page)
    await waitUntilEqual(() => assistanceNeedDecisionsPage.rows.count(), 1)
    await waitUntilEqual(() => assistanceNeedDecisionsPage.row(0), {
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: false
    })

    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${decisionId}`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)
    await decisionPage.decisionMaker.assertTextEquals(
      `${decisionMaker.firstName} ${decisionMaker.lastName}, regional director`
    )
  })
})
