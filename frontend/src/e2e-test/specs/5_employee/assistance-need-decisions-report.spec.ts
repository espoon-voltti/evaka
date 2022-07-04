// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  careAreaFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { AssistanceNeedDecision, EmployeeDetail } from '../../dev-api/types'
import {
  AssistanceNeedDecisionsReport,
  AssistanceNeedDecisionsReportDecision
} from '../../pages/employee/reports'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let decisionMaker: EmployeeDetail
let director: EmployeeDetail
let decisions: AssistanceNeedDecision[]

beforeAll(async () => {
  await resetDatabase()

  decisionMaker = (await Fixture.employeeAdmin().save()).data
  director = (await Fixture.employeeDirector().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id

  const childId = familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  decisions = await Promise.all(
    [
      Fixture.assistanceNeedDecision()
        .withChild(childId)
        .with({
          decisionMaker: {
            employeeId: decisionMaker.id,
            title: 'regional director'
          },
          sentForDecision: LocalDate.of(2020, 6, 8),
          selectedUnit: { id: unitId }
        }),
      Fixture.assistanceNeedDecision()
        .withChild(childId)
        .with({
          decisionMaker: {
            employeeId: decisionMaker.id,
            title: 'regional director'
          },
          sentForDecision: LocalDate.of(2021, 1, 6),
          selectedUnit: { id: unitId }
        }),
      Fixture.assistanceNeedDecision()
        .withChild(childId)
        .with({
          decisionMaker: {
            employeeId: director.id,
            title: 'director'
          },
          sentForDecision: LocalDate.of(2020, 1, 1),
          selectedUnit: { id: unitId }
        }),
      Fixture.assistanceNeedDecision()
        .withChild(childId)
        .with({
          decisionMaker: {
            employeeId: decisionMaker.id,
            title: 'regional director'
          },
          sentForDecision: LocalDate.of(2019, 9, 6),
          selectedUnit: { id: unitId }
        })
    ].map(async (d) => (await d.save()).data)
  )

  await insertDaycarePlacementFixtures([daycarePlacementFixture])
})

describe('Assistance need decisions report', () => {
  beforeEach(async () => {
    page = await Page.open()
  })

  const baseReportRow = {
    childName: `${familyWithTwoGuardians.children[0].lastName} ${familyWithTwoGuardians.children[0].firstName}`,
    careAreaName: careAreaFixture.name,
    unitName: daycareFixture.name,
    decisionMade: '-',
    status: 'DRAFT'
  }

  test('Lists correct decisions', async () => {
    await employeeLogin(page, decisionMaker)
    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)
    const report = new AssistanceNeedDecisionsReport(page)

    expect(await report.row(0)).toMatchObject({
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: true
    })
    expect(await report.row(1)).toMatchObject({
      ...baseReportRow,
      sentForDecision: '08.06.2020',
      isUnopened: true
    })
    expect(await report.row(2)).toMatchObject({
      ...baseReportRow,
      sentForDecision: '01.01.2020',
      isUnopened: false // different decision-maker's decision
    })
    expect(await report.row(3)).toMatchObject({
      ...baseReportRow,
      sentForDecision: '06.09.2019',
      isUnopened: true
    })
  })

  test('Removes unopened indicator from decision after opening decision', async () => {
    await employeeLogin(page, decisionMaker)
    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)
    expect(await new AssistanceNeedDecisionsReport(page).row(0)).toMatchObject({
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: true
    })

    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${
        decisions[1].id ?? ''
      }`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)
    await waitUntilEqual(
      decisionPage.decisionMaker,
      `${decisionMaker.firstName} ${decisionMaker.lastName}, regional director`
    )

    await page.goto(`${config.employeeUrl}/reports/assistance-need-decisions`)

    expect(await new AssistanceNeedDecisionsReport(page).row(0)).toMatchObject({
      ...baseReportRow,
      sentForDecision: '06.01.2021',
      isUnopened: false
    })
  })

  test('Returns decision for editing', async () => {
    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${
        decisions[1].id ?? ''
      }`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.returnForEditBtn.click()
    await decisionPage.returnForEditModal.okBtn.click()

    await waitUntilEqual(decisionPage.decisionStatus, 'NEEDS_WORK')
  })

  test('Decision can be rejected', async () => {
    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${
        decisions[0].id ?? ''
      }`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.rejectBtn.click()
    await decisionPage.modalOkBtn.click()

    await waitUntilEqual(decisionPage.decisionStatus, 'REJECTED')
  })

  test('Decision can be accepted', async () => {
    await employeeLogin(page, decisionMaker)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${
        decisions[1].id ?? ''
      }`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await decisionPage.approveBtn.click()
    await decisionPage.modalOkBtn.click()

    await waitUntilEqual(decisionPage.decisionStatus, 'ACCEPTED')
  })

  test('Decision-maker can be changed', async () => {
    await employeeLogin(page, director)
    await page.goto(
      `${config.employeeUrl}/reports/assistance-need-decisions/${
        decisions[3].id ?? ''
      }`
    )

    const decisionPage = new AssistanceNeedDecisionsReportDecision(page)

    await waitUntilEqual(
      decisionPage.decisionMaker,
      `${decisionMaker.firstName} ${decisionMaker.lastName}, regional director`
    )

    expect(await decisionPage.approveBtn.getAttribute('disabled')).toBe('')

    await decisionPage.mismatchModalLink.click()
    await decisionPage.mismatchModal.titleInput.type('director')
    await decisionPage.mismatchModal.okBtn.click()

    await waitUntilEqual(
      decisionPage.decisionMaker,
      `${director.firstName} ${director.lastName}, director`
    )

    expect(await decisionPage.approveBtn.getAttribute('disabled')).toBe(null)
  })
})
