// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { DecisionIncome } from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  testCareArea2,
  testDaycare2,
  testDaycare,
  DecisionIncomeFixture,
  testChild2,
  testAdult,
  familyWithTwoGuardians,
  feeDecisionsFixture,
  Fixture,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createFeeDecisions,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import type { FeeDecisionsPage } from '../../pages/employee/finance/finance-page'
import { FinancePage } from '../../pages/employee/finance/finance-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let feeDecisionsPage: FeeDecisionsPage

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()
  await familyWithTwoGuardians.save()
  const careArea = await testCareArea2.save()
  await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
})

const insertFeeDecisionFixtureAndNavigateToIt = async (
  headOfFamily: DevPerson,
  child: DevPerson,
  validDuring: FiniteDateRange,
  partner: DevPerson | null = null,
  childIncome: DecisionIncome | null = null
) => {
  const fd = feeDecisionsFixture(
    'DRAFT',
    headOfFamily,
    child,
    testDaycare.id,
    partner,
    validDuring
  )

  await createFeeDecisions({
    body: [
      {
        ...fd,
        children: fd.children.map((child) => ({
          ...child,
          childIncome
        }))
      }
    ]
  })

  await new EmployeeNav(page).openTab('finance')
  feeDecisionsPage = await new FinancePage(page).selectFeeDecisionsTab()
}

describe('Fee decisions', () => {
  beforeEach(async () => {
    const financeAdmin = await Fixture.employee().financeAdmin().save()

    page = await Page.open({ acceptDownloads: true })
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
  })

  test('List of fee decision drafts shows at least one row', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    await waitUntilEqual(() => feeDecisionsPage.getFeeDecisionCount(), 1)
  })

  test('Navigate to the from decision details page', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    await feeDecisionsPage.openFirstFeeDecision()
  })

  test('Fee decisions are toggled and sent', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    await feeDecisionsPage.sendFeeDecisions(HelsinkiDateTime.of(2023, 1, 1))
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await feeDecisionsPage.assertSentDecisionsCount(1)
  })

  test('Partner is shown for elementary family', async () => {
    const partner = familyWithTwoGuardians.otherGuardian!
    await insertGuardians({
      body: [
        {
          guardianId: familyWithTwoGuardians.guardian.id,
          childId: familyWithTwoGuardians.children[0].id
        },
        {
          guardianId: partner.id,
          childId: familyWithTwoGuardians.children[0].id
        }
      ]
    })
    await insertFeeDecisionFixtureAndNavigateToIt(
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.children[0],
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
      partner
    )
    await feeDecisionsPage.searchButton.click()
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertPartnerName(
      `${partner.firstName} ${partner.lastName}`
    )
  })

  test('Partner is not shown for non elementary family', async () => {
    const partner = familyWithTwoGuardians.otherGuardian
    await insertGuardians({
      body: [
        {
          guardianId: familyWithTwoGuardians.guardian.id,
          childId: familyWithTwoGuardians.children[0].id
        }
      ]
    })
    await insertFeeDecisionFixtureAndNavigateToIt(
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.children[0],
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
      partner
    )
    await feeDecisionsPage.searchButton.click()
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertPartnerNameNotShown()
  })

  test('Child income is shown', async () => {
    const partner = familyWithTwoGuardians.otherGuardian!
    await insertGuardians({
      body: [
        {
          guardianId: familyWithTwoGuardians.guardian.id,
          childId: familyWithTwoGuardians.children[0].id
        },
        {
          guardianId: partner.id,
          childId: familyWithTwoGuardians.children[0].id
        }
      ]
    })
    await insertFeeDecisionFixtureAndNavigateToIt(
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.children[0],
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
      partner,
      DecisionIncomeFixture(54321)
    )
    await feeDecisionsPage.searchButton.click()
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertChildIncome(0, '543,21 €')
  })
})

describe('Fee decisions with finance decision handler select enabled', () => {
  beforeEach(async () => {
    const financeAdmin = await Fixture.employee({
      firstName: 'Lasse',
      lastName: 'Laskuttaja'
    })
      .financeAdmin()
      .save()

    page = await Page.open({
      acceptDownloads: true,
      employeeCustomizations: {
        featureFlags: {
          financeDecisionHandlerSelect: true
        }
      }
    })
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
  })

  test('Fee decisions are toggled and cancelled', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    const modal = await feeDecisionsPage.openDecisionHandlerModal()
    await modal.rejectDecisionHandlerModal(HelsinkiDateTime.of(2023, 1, 1))
    await feeDecisionsPage.assertSentDecisionsCount(0)
  })

  test('Fee decisions are toggled and sent without selecting decision handler', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    const modal = await feeDecisionsPage.openDecisionHandlerModal()
    await modal.resolveDecisionHandlerModal(HelsinkiDateTime.of(2023, 5, 3))
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await feeDecisionsPage.assertSentDecisionsCount(1)
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertDecisionHandler('Lasse Laskuttaja')
  })

  test('Fee decisions are toggled and sent with selecting decision handler', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    const otherFinanceAdmin = await Fixture.employee({
      email: 'laura.laskuttaja@evaka.test',
      firstName: 'Laura',
      lastName: 'Laskuttaja'
    })
      .financeAdmin()
      .save()
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    const modal = await feeDecisionsPage.openDecisionHandlerModal()
    await modal.selectDecisionHandler(otherFinanceAdmin.id)
    await modal.resolveDecisionHandlerModal(HelsinkiDateTime.of(2023, 5, 3))
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await feeDecisionsPage.assertSentDecisionsCount(1)
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertDecisionHandler('Laura Laskuttaja')
  })

  test('Fee decision is sent without selecting decision handler', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    const feeDecisionDetailsPageDraft =
      await feeDecisionsPage.openFirstFeeDecision()
    const modal = await feeDecisionDetailsPageDraft.openDecisionHandlerModal()
    await modal.resolveDecisionHandlerModal(HelsinkiDateTime.of(2023, 5, 3))
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await feeDecisionDetailsPageDraft.assertDecisionHandler('Lasse Laskuttaja')
    await feeDecisionsPage.assertSentDecisionsCount(1)
    const feeDecisionDetailsPageSent =
      await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPageSent.assertDecisionHandler('Lasse Laskuttaja')
  })

  test('Fee decision is sent with selecting decision handler', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      testAdult,
      testChild2,
      new FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.searchButton.click()
    const otherFinanceAdmin = await Fixture.employee({
      email: 'laura.laskuttaja@evaka.test',
      firstName: 'Laura',
      lastName: 'Laskuttaja'
    })
      .financeAdmin()
      .save()
    const feeDecisionDetailsPageDraft =
      await feeDecisionsPage.openFirstFeeDecision()
    const modal = await feeDecisionDetailsPageDraft.openDecisionHandlerModal()
    await modal.selectDecisionHandler(otherFinanceAdmin.id)
    await modal.resolveDecisionHandlerModal(HelsinkiDateTime.of(2023, 5, 3))
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await feeDecisionDetailsPageDraft.assertDecisionHandler('Laura Laskuttaja')
    await feeDecisionsPage.assertSentDecisionsCount(1)
    const feeDecisionDetailsPageSent =
      await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPageSent.assertDecisionHandler('Laura Laskuttaja')
  })
})
