// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careArea2Fixture,
  daycare2Fixture,
  daycareFixture,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  familyWithTwoGuardians,
  Fixture,
  voucherValueDecisionsFixture
} from 'e2e-test-common/dev-api/fixtures'
import {
  insertEmployeeFixture,
  insertGuardianFixtures,
  insertVoucherValueDecisionFixtures,
  resetDatabase,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import {
  FinancePage,
  ValueDecisionDetailsPage,
  ValueDecisionsPage
} from 'e2e-playwright/pages/employee/finance/finance-page'
import LocalDate from 'lib-common/local-date'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'

let page: Page
let valueDecisionsPage: ValueDecisionsPage
const decision1DateFrom = LocalDate.today().subWeeks(1)
const decision1DateTo = LocalDate.today().addWeeks(2)
const decision2DateFrom = LocalDate.today()
const decision2DateTo = LocalDate.today().addWeeks(5)

beforeEach(async () => {
  await resetDatabase()
  await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()

  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()

  await insertEmployeeFixture({
    id: config.financeAdminAad,
    externalId: `espoo-ad:${config.financeAdminAad}`,
    email: 'lasse.laskuttaja@evaka.test',
    firstName: 'Lasse',
    lastName: 'Laskuttaja',
    roles: ['FINANCE_ADMIN']
  })
  await employeeLogin(page, 'FINANCE_ADMIN')
  await page.goto(config.employeeUrl)
})

const insertTwoValueDecisionsFixturesAndNavigateToValueDecisions = async () => {
  await insertVoucherValueDecisionFixtures([
    voucherValueDecisionsFixture(
      'e2d75fa4-7359-406b-81b8-1703785ca649',
      enduserGuardianFixture.id,
      enduserChildFixtureKaarina.id,
      daycareFixture.id,
      null,
      'DRAFT',
      decision1DateFrom.formatIso(),
      decision1DateTo.formatIso()
    ),
    voucherValueDecisionsFixture(
      'ed462aca-f74e-4384-910f-628823201023',
      enduserGuardianFixture.id,
      enduserChildFixtureJari.id,
      daycare2Fixture.id,
      null,
      'DRAFT',
      decision2DateFrom.formatIso(),
      decision2DateTo.formatIso()
    )
  ])
  await new EmployeeNav(page).openTab('finance')
  valueDecisionsPage = await new FinancePage(page).selectValueDecisionsTab()
}

const insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions =
  async () => {
    await insertVoucherValueDecisionFixtures([
      voucherValueDecisionsFixture(
        'e2d75fa4-7359-406b-81b8-1703785ca649',
        familyWithTwoGuardians.guardian.id,
        familyWithTwoGuardians.children[0].id,
        daycareFixture.id,
        familyWithTwoGuardians.otherGuardian,
        'DRAFT',
        decision1DateFrom.formatIso(),
        decision1DateTo.formatIso()
      )
    ])
    await new EmployeeNav(page).openTab('finance')
    valueDecisionsPage = await new FinancePage(page).selectValueDecisionsTab()
  }

describe('Value decisions', () => {
  test('Date filter filters out decisions', async () => {
    await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions()

    await valueDecisionsPage.setDates(
      decision1DateFrom.subDays(1),
      decision2DateTo.addDays(1)
    )
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)

    await valueDecisionsPage.setDates(
      decision1DateTo.addDays(1),
      decision2DateTo.addDays(1)
    )
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 1)
  })

  test('With two decisions any date filter overlap will show the decision', async () => {
    await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions()

    await valueDecisionsPage.setDates(
      decision1DateTo.subDays(1),
      decision2DateTo.subDays(1)
    )
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)
  })

  test('Start date checkbox will filter out decisions that do not have a startdate within the date range', async () => {
    await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions()

    await valueDecisionsPage.setDates(
      decision2DateFrom.subDays(1),
      decision2DateTo.subDays(1)
    )
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)
    await valueDecisionsPage.startDateWithinRange()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 1)
  })

  test('Navigate to and from decision details page', async () => {
    await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions()

    await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionsPage.navigateBackFromDetails()
  })

  test('Send value decision from details page', async () => {
    await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions()

    await valueDecisionsPage.openFirstValueDecision()
    await new ValueDecisionDetailsPage(page).sendValueDecision()
    await runPendingAsyncJobs()
    await valueDecisionsPage.navigateBackFromDetails()
    await valueDecisionsPage.assertSentDecisionsCount(1)
  })

  test('Voucher value decisions are toggled and sent', async () => {
    await insertTwoValueDecisionsFixturesAndNavigateToValueDecisions()

    await valueDecisionsPage.toggleAllValueDecisions(true)
    await valueDecisionsPage.sendValueDecisions()
    await valueDecisionsPage.assertSentDecisionsCount(2)
  })

  test('Partner is shown for elementary family', async () => {
    await insertGuardianFixtures([
      {
        guardianId: familyWithTwoGuardians.guardian.id,
        childId: familyWithTwoGuardians.children[0].id
      },
      {
        guardianId: familyWithTwoGuardians.otherGuardian.id,
        childId: familyWithTwoGuardians.children[0].id
      }
    ])
    await insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions()

    await valueDecisionsPage.openFirstValueDecision()
    await new ValueDecisionDetailsPage(page).assertPartnerName(
      `${familyWithTwoGuardians.otherGuardian.firstName} ${familyWithTwoGuardians.otherGuardian.lastName}`
    )
  })

  test('Partner is not shown for non elementary family', async () => {
    await insertGuardianFixtures([
      {
        guardianId: familyWithTwoGuardians.guardian.id,
        childId: familyWithTwoGuardians.children[0].id
      }
    ])
    await insertValueDecisionWithPartnerFixtureAndNavigateToValueDecisions()

    await valueDecisionsPage.openFirstValueDecision()
    await new ValueDecisionDetailsPage(page).assertPartnerNameNotShown()
  })
})
