// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DecisionIncome } from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  testCareArea2,
  testDaycare2,
  testDaycare,
  DecisionIncomeFixture,
  testChild,
  testChild2,
  testAdult,
  familyWithTwoGuardians,
  Fixture,
  voucherValueDecisionsFixture,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createVoucherValueDecisions,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import type { ValueDecisionsPage } from '../../pages/employee/finance/finance-page'
import { FinancePage } from '../../pages/employee/finance/finance-page'
import { test } from '../../playwright'
import { waitUntilEqual } from '../../utils'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const now = HelsinkiDateTime.of(2020, 1, 1, 15, 0)

const decision1DateFrom = now.toLocalDate().subWeeks(1)
const decision1DateTo = now.toLocalDate().addWeeks(2)
const decision2DateFrom = now.toLocalDate()
const decision2DateTo = now.toLocalDate().addWeeks(5)

test.use({ evakaOptions: { mockedTime: now } })

async function setupFixtures() {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await familyWithTwoGuardians.save()
  const careArea = await testCareArea2.save()
  await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
}

const insertTwoValueDecisionsFixturesAndNavigateToValueDecisions = async (
  page: Page
): Promise<ValueDecisionsPage> => {
  await createVoucherValueDecisions({
    body: [
      voucherValueDecisionsFixture(
        fromUuid('e2d75fa4-7359-406b-81b8-1703785ca649'),
        testAdult.id,
        testChild2.id,
        testDaycare.id,
        null,
        'DRAFT',
        decision1DateFrom,
        decision1DateTo
      ),
      voucherValueDecisionsFixture(
        fromUuid('ed462aca-f74e-4384-910f-628823201023'),
        testAdult.id,
        testChild.id,
        testDaycare2.id,
        null,
        'DRAFT',
        decision2DateFrom,
        decision2DateTo
      )
    ]
  })
  await new EmployeeNav(page).openTab('finance')
  return await new FinancePage(page).selectValueDecisionsTab()
}

const insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions = async (
  page: Page,
  childIncome: DecisionIncome | null = null
): Promise<ValueDecisionsPage> => {
  const decision = voucherValueDecisionsFixture(
    fromUuid('e2d75fa4-7359-406b-81b8-1703785ca649'),
    familyWithTwoGuardians.guardian.id,
    familyWithTwoGuardians.children[0].id,
    testDaycare.id,
    familyWithTwoGuardians.otherGuardian,
    'DRAFT',
    decision1DateFrom,
    decision1DateTo
  )

  await createVoucherValueDecisions({
    body: [
      {
        ...decision,
        childIncome
      }
    ]
  })
  await new EmployeeNav(page).openTab('finance')
  return await new FinancePage(page).selectValueDecisionsTab()
}

test.describe('Value decisions', () => {
  let page: Page
  let valueDecisionsPage: ValueDecisionsPage

  test.beforeEach(async ({ evaka }) => {
    await setupFixtures()
    const financeAdmin = await Fixture.employee().financeAdmin().save()
    page = evaka
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
  })

  test('Date filter filters out decisions', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)

    await valueDecisionsPage.setDates(
      decision1DateFrom.subDays(1),
      decision2DateTo.addDays(1)
    )
    await valueDecisionsPage.searchButton.click()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)

    await valueDecisionsPage.setDates(
      decision1DateTo.addDays(1),
      decision2DateTo.addDays(1)
    )
    await valueDecisionsPage.searchButton.click()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 1)
  })

  test('With two decisions any date filter overlap will show the decision', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)

    await valueDecisionsPage.setDates(
      decision1DateTo.subDays(1),
      decision2DateTo.subDays(1)
    )
    await valueDecisionsPage.searchButton.click()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)
  })

  test('Start date checkbox will filter out decisions that do not have a startdate within the date range', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)

    await valueDecisionsPage.setDates(
      decision2DateFrom.subDays(1),
      decision2DateTo.subDays(1)
    )
    await valueDecisionsPage.searchButton.click()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)
    await valueDecisionsPage.startDateWithinRange()
    await valueDecisionsPage.searchButton.click()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 1)
  })

  test('Navigate to the decision details page', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    await valueDecisionsPage.openFirstValueDecision()
  })

  test('Send value decision from details page', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    const valueDecisionDetailsPage =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPage.sendValueDecision()
    await runPendingAsyncJobs(now)
    await valueDecisionsPage.assertSentDecisionsCount(1)
  })

  test('Voucher value decisions are toggled and sent', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    await valueDecisionsPage.toggleAllValueDecisions()
    await valueDecisionsPage.sendValueDecisions(now)
    await valueDecisionsPage.assertSentDecisionsCount(2)
  })

  test('Partner is shown for elementary family', async () => {
    await insertGuardians({
      body: [
        {
          guardianId: familyWithTwoGuardians.guardian.id,
          childId: familyWithTwoGuardians.children[0].id
        },
        {
          guardianId: familyWithTwoGuardians.otherGuardian!.id,
          childId: familyWithTwoGuardians.children[0].id
        }
      ]
    })
    valueDecisionsPage =
      await insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions(
        page
      )
    await valueDecisionsPage.searchButton.click()

    const valueDecisionDetailsPage =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPage.assertPartnerName(
      `${familyWithTwoGuardians.otherGuardian!.firstName} ${familyWithTwoGuardians.otherGuardian!.lastName}`
    )
  })

  test('Partner is not shown for non elementary family', async () => {
    await insertGuardians({
      body: [
        {
          guardianId: familyWithTwoGuardians.guardian.id,
          childId: familyWithTwoGuardians.children[0].id
        }
      ]
    })
    valueDecisionsPage =
      await insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions(
        page
      )
    await valueDecisionsPage.searchButton.click()

    const valueDecisionDetailsPage =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPage.assertPartnerNameNotShown()
  })

  test('Child income is shown', async () => {
    await insertGuardians({
      body: [
        {
          guardianId: familyWithTwoGuardians.guardian.id,
          childId: familyWithTwoGuardians.children[0].id
        }
      ]
    })
    valueDecisionsPage =
      await insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions(
        page,
        DecisionIncomeFixture(54321)
      )
    await valueDecisionsPage.searchButton.click()

    const valueDecisionDetailsPage =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPage.assertChildIncome(0, '543,21 €')
  })
})

test.describe('Value decisions with finance decision handler select enabled', () => {
  test.use({
    evakaOptions: {
      mockedTime: now,
      employeeCustomizations: {
        featureFlags: {
          financeDecisionHandlerSelect: true
        }
      }
    }
  })

  let page: Page
  let valueDecisionsPage: ValueDecisionsPage

  test.beforeEach(async ({ evaka }) => {
    await setupFixtures()
    const financeAdmin = await Fixture.employee({
      firstName: 'Lasse',
      lastName: 'Laskuttaja'
    })
      .financeAdmin()
      .save()
    page = evaka
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
  })

  test('Voucher value decisions are toggled and cancelled', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    await valueDecisionsPage.toggleAllValueDecisions()
    const modal = await valueDecisionsPage.openDecisionHandlerModal()
    await modal.rejectDecisionHandlerModal(now)
    await valueDecisionsPage.assertSentDecisionsCount(0)
  })

  test('Voucher value decisions are toggled and sent without selecting decision handler', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    await valueDecisionsPage.toggleAllValueDecisions()
    const modal = await valueDecisionsPage.openDecisionHandlerModal()
    await modal.resolveDecisionHandlerModal(now)
    await valueDecisionsPage.assertSentDecisionsCount(2)
    const valueDecisionDetailsPage =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPage.assertDecisionHandler('Lasse Laskuttaja')
  })

  test('Voucher value decisions are toggled and sent with selecting decision handler', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    const otherFinanceAdmin = await Fixture.employee({
      email: 'laura.laskuttaja@evaka.test',
      firstName: 'Laura',
      lastName: 'Laskuttaja'
    })
      .financeAdmin()
      .save()
    await valueDecisionsPage.toggleAllValueDecisions()
    const modal = await valueDecisionsPage.openDecisionHandlerModal()
    await modal.selectDecisionHandler(otherFinanceAdmin.id)
    await modal.resolveDecisionHandlerModal(now)
    await valueDecisionsPage.assertSentDecisionsCount(2)
    const valueDecisionDetailsPage =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPage.assertDecisionHandler('Laura Laskuttaja')
  })

  test('Voucher value decision is sent without selecting decision handler', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    const valueDecisionDetailsPageDraft =
      await valueDecisionsPage.openFirstValueDecision()
    const modal = await valueDecisionDetailsPageDraft.openDecisionHandlerModal()
    await modal.resolveDecisionHandlerModal(now)
    await valueDecisionDetailsPageDraft.assertDecisionHandler(
      'Lasse Laskuttaja'
    )
    await valueDecisionsPage.assertSentDecisionsCount(1)
    const valueDecisionDetailsPageSent =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPageSent.assertDecisionHandler('Lasse Laskuttaja')
  })

  test('Voucher value decision is sent with selecting decision handler', async () => {
    valueDecisionsPage =
      await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions(page)
    await valueDecisionsPage.searchButton.click()
    const otherFinanceAdmin = await Fixture.employee({
      email: 'laura.laskuttaja@evaka.test',
      firstName: 'Laura',
      lastName: 'Laskuttaja'
    })
      .financeAdmin()
      .save()
    const valueDecisionDetailsPageDraft =
      await valueDecisionsPage.openFirstValueDecision()
    const modal = await valueDecisionDetailsPageDraft.openDecisionHandlerModal()
    await modal.selectDecisionHandler(otherFinanceAdmin.id)
    await modal.resolveDecisionHandlerModal(now)
    await valueDecisionDetailsPageDraft.assertDecisionHandler(
      'Laura Laskuttaja'
    )
    await valueDecisionsPage.assertSentDecisionsCount(1)
    const valueDecisionDetailsPageSent =
      await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionDetailsPageSent.assertDecisionHandler('Laura Laskuttaja')
  })
})
