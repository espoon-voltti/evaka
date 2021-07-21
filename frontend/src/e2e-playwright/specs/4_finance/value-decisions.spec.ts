// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careArea2Fixture,
  daycare2Fixture,
  Fixture,
  voucherValueDecisionsFixture
} from 'e2e-test-common/dev-api/fixtures'
import {
  insertEmployeeFixture,
  insertVoucherValueDecisionFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import {
  FinancePage,
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
  const fixtures = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()

  await insertVoucherValueDecisionFixtures([
    voucherValueDecisionsFixture(
      'e2d75fa4-7359-406b-81b8-1703785ca649',
      fixtures.enduserGuardianFixture.id,
      fixtures.enduserChildFixtureKaarina.id,
      fixtures.daycareFixture.id,
      'DRAFT',
      decision1DateFrom.formatIso(),
      decision1DateTo.formatIso()
    ),
    voucherValueDecisionsFixture(
      'ed462aca-f74e-4384-910f-628823201023',
      fixtures.enduserGuardianFixture.id,
      fixtures.enduserChildFixtureJari.id,
      daycare2Fixture.id,
      'DRAFT',
      decision2DateFrom.formatIso(),
      decision2DateTo.formatIso()
    )
  ])

  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()

  await insertEmployeeFixture({
    id: config.financeAdminAad,
    externalId: `espoo-ad:${config.financeAdminAad}`,
    email: 'lasse.laskuttaja@espoo.fi',
    firstName: 'Lasse',
    lastName: 'Laskuttaja',
    roles: ['FINANCE_ADMIN']
  })
  await employeeLogin(page, 'FINANCE_ADMIN')

  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')
  valueDecisionsPage = await new FinancePage(page).selectValueDecisionsTab()
})
afterEach(async () => {
  await page.close()
})

describe('Value decisions', () => {
  test('Date filter filters out decisions', async () => {
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
    await valueDecisionsPage.setDates(
      decision1DateTo.subDays(1),
      decision2DateTo.subDays(1)
    )
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)
  })

  test('Start date checkbox will filter out decisions that do not have a startdate within the date range', async () => {
    await valueDecisionsPage.setDates(
      decision2DateFrom.subDays(1),
      decision2DateTo.subDays(1)
    )
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 2)
    await valueDecisionsPage.startDateWithinrange()
    await waitUntilEqual(() => valueDecisionsPage.getValueDecisionCount(), 1)
  })

  test('Navigate to and from decision details page', async () => {
    await valueDecisionsPage.openFirstValueDecision()
    await valueDecisionsPage.navigateBackFromDetails()
  })

  test('Voucher value decisions are toggled and sent', async () => {
    await valueDecisionsPage.toggleAllValueDecisions(true)
    await valueDecisionsPage.sendValueDecisions()
    await valueDecisionsPage.assertSentDecisionsCount(2)
  })
})
