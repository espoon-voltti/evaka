// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import { DecisionIncome } from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  careArea2Fixture,
  daycare2Fixture,
  daycareFixture,
  DecisionIncomeFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  familyWithTwoGuardians,
  feeDecisionsFixture,
  Fixture
} from '../../dev-api/fixtures'
import {
  createFeeDecisions,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  FeeDecisionsPage,
  FinancePage
} from '../../pages/employee/finance/finance-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let feeDecisionsPage: FeeDecisionsPage

beforeEach(async () => {
  await resetServiceState()
  await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
})

const insertFeeDecisionFixtureAndNavigateToIt = async (
  headOfFamily: DevPerson,
  child: DevPerson,
  validDuring: DateRange,
  partner: DevPerson | null = null,
  childIncome: DecisionIncome | null = null
) => {
  const fd = feeDecisionsFixture(
    'DRAFT',
    headOfFamily,
    child,
    daycareFixture.id,
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
    const financeAdmin = await Fixture.employeeFinanceAdmin().save()

    page = await Page.open({ acceptDownloads: true })
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
  })

  test('List of fee decision drafts shows at least one row', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await waitUntilEqual(() => feeDecisionsPage.getFeeDecisionCount(), 1)
  })

  test('Navigate to the from decision details page', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.openFirstFeeDecision()
  })

  test('Fee decisions are toggled and sent', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    await feeDecisionsPage.sendFeeDecisions(HelsinkiDateTime.of(2023, 1, 1))
    await runPendingAsyncJobs(HelsinkiDateTime.now())
    await feeDecisionsPage.assertSentDecisionsCount(1)
  })

  test('Partner is shown for elementary family', async () => {
    const partner = familyWithTwoGuardians.otherGuardian
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
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
      partner
    )
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
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
      partner
    )
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertPartnerNameNotShown()
  })

  test('Child income is shown', async () => {
    const partner = familyWithTwoGuardians.otherGuardian
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
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
      partner,
      DecisionIncomeFixture(54321)
    )
    const feeDecisionDetailsPage = await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionDetailsPage.assertChildIncome(0, '543,21 €')
  })
})

describe('Fee decisions with finance decision handler select enabled', () => {
  beforeEach(async () => {
    const financeAdmin = await Fixture.employeeFinanceAdmin().save()

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
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    const modal = await feeDecisionsPage.openDecisionHandlerModal()
    await modal.rejectDecisionHandlerModal(HelsinkiDateTime.of(2023, 1, 1))
    await feeDecisionsPage.assertSentDecisionsCount(0)
  })

  test('Fee decisions are toggled and sent without selecting decision handler', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
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
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    const otherFinanceAdmin = await Fixture.employeeFinanceAdmin()
      .with({
        email: 'laura.laskuttaja@evaka.test',
        firstName: 'Laura',
        lastName: 'Laskuttaja'
      })
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
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
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
      enduserGuardianFixture,
      enduserChildFixtureKaarina,
      new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
    )
    const otherFinanceAdmin = await Fixture.employeeFinanceAdmin()
      .with({
        email: 'laura.laskuttaja@evaka.test',
        firstName: 'Laura',
        lastName: 'Laskuttaja'
      })
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
