// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careArea2Fixture,
  daycare2Fixture,
  feeDecisionsFixture,
  Fixture
} from 'e2e-test-common/dev-api/fixtures'
import {
  insertEmployeeFixture,
  insertFeeDecisionFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import {
  FinancePage,
  FeeDecisionsPage
} from 'e2e-playwright/pages/employee/finance/finance-page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'

let page: Page
let feeDecisionsPage: FeeDecisionsPage

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()

  await insertFeeDecisionFixtures([
    feeDecisionsFixture(
      'DRAFT',
      fixtures.enduserGuardianFixture,
      fixtures.enduserChildFixtureKaarina,
      fixtures.daycareFixture.id
    )
  ])

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
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')
  feeDecisionsPage = await new FinancePage(page).selectFeeDecisionsTab()
})
afterEach(async () => {
  await page.close()
})

describe('Fee decisions', () => {
  test('List of fee decision drafts shows at least one row', async () => {
    await waitUntilEqual(() => feeDecisionsPage.getFeeDecisionCount(), 1)
  })

  test('Navigate to and from decision details page', async () => {
    await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionsPage.navigateBackFromDetails()
  })

  test('Fee decisions are toggled and sent', async () => {
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    await feeDecisionsPage.sendFeeDecisions()
    await feeDecisionsPage.assertSentDecisionsCount(1)
  })
})
