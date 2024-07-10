// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  testCareArea2,
  testCareArea,
  createDaycarePlacementFixture,
  testDaycare2,
  testDaycare,
  testDaycareGroup,
  testChild,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
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

let unitId: UUID
let childId: UUID

const mockedTime = LocalDate.of(2021, 8, 16)

beforeEach(async () => {
  await resetServiceState()

  decisionMaker = await Fixture.employeeAdmin().save()
  director = await Fixture.employeeDirector().save()

  const fixtures = await initializeAreaAndPersonData()
  await createDaycareGroups({ body: [testDaycareGroup] })

  unitId = fixtures.testDaycare.id
  childId = familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
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
    await Fixture.assistanceNeedDecision()
      .with({
        id: uuidv4(),
        childId,
        decisionMaker: {
          employeeId: decisionMaker.id,
          title: 'regional director',
          name: null,
          phoneNumber: null
        },
        sentForDecision: LocalDate.of(2021, 1, 6),
        selectedUnit: unitId
      })
      .save()
    await Fixture.assistanceNeedDecision()
      .withChild(childId)
      .with({
        id: uuidv4(),
        childId,
        decisionMaker: {
          employeeId: decisionMaker.id,
          title: 'regional director',
          name: null,
          phoneNumber: null
        },
        sentForDecision: LocalDate.of(2020, 6, 8),
        selectedUnit: unitId
      })
      .save()
    await Fixture.assistanceNeedDecision()
      .withChild(childId)
      .with({
        decisionMaker: {
          employeeId: director.id,
          title: 'director',
          name: null,
          phoneNumber: null
        },
        sentForDecision: LocalDate.of(2020, 1, 1),
        selectedUnit: unitId
      })
      .save()
    await Fixture.assistanceNeedDecision()
      .with({
        id: uuidv4(),
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
      })
      .save()

    await employeeLogin(page, decisionMaker)
    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)
    const report = new AssistanceNeedDecisionsReport(page)

    await report.rows.assertCount(4)
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
      sentForDecision: '01.01.2020',
      isUnopened: false // different decision-maker's decision
    })
    await waitUntilEqual(() => report.row(3), {
      ...baseReportRow,
      status: 'ACCEPTED',
      sentForDecision: '06.09.2019',
      isUnopened: true
    })
  })

  test('Removes unopened indicator from decision after opening decision', async () => {
    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .with({
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
      })
      .save()

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
    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .with({
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
      })
      .save()

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
    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .withChild(childId)
      .with({
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
      })
      .save()
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
    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .with({
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
      })
      .save()
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
    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .with({
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
      })
      .save()
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
    const admin = await Fixture.employeeAdmin()
      .with({ id: uuidv4(), firstName: 'Sari', lastName: 'Sorsa' })
      .save()

    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .with({
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
      })
      .save()

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
    const decisionId = uuidv4()
    await Fixture.assistanceNeedDecision()
      .with({
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
      })
      .save()

    const anotherChildId = testChild.id
    const careArea = await Fixture.careArea().with(testCareArea2).save()
    const anotherDaycare = await Fixture.daycare()
      .with(testDaycare2)
      .careArea(careArea)
      .save()

    const daycarePlacementFixture2 = createDaycarePlacementFixture(
      uuidv4(),
      anotherChildId,
      anotherDaycare.id
    )

    await createDaycarePlacements({ body: [daycarePlacementFixture2] })

    await Fixture.assistanceNeedDecision()
      .with({
        id: uuidv4(),
        childId: anotherChildId,
        decisionMaker: {
          employeeId: decisionMaker.id,
          title: 'regional director',
          name: null,
          phoneNumber: null
        },
        sentForDecision: LocalDate.of(2021, 1, 6),
        selectedUnit: anotherDaycare.id
      })
      .save()

    const VEO = await Fixture.employeeSpecialEducationTeacher(unitId).save()

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
