// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InvoicingPage from '../../pages/invoicing'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { voucherValueDecisionsFixture } from 'e2e-test-common/dev-api/fixtures'
import {
  insertVoucherValueDecisionFixtures,
  resetDatabase,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import { logConsoleMessages } from '../../utils/fixture'
import { employeeLogin, seppoAdmin } from '../../config/users'

const page = new InvoicingPage()

let fixtures: AreaAndPersonFixtures

fixture('Invoicing - voucher value decisions')
  .meta({ type: 'regression', subType: 'voucherValueDecisions' })
  .beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()

    await insertVoucherValueDecisionFixtures([
      voucherValueDecisionsFixture(
        'e2d75fa4-7359-406b-81b8-1703785ca649',
        fixtures.enduserGuardianFixture.id,
        fixtures.enduserChildFixtureKaarina.id,
        fixtures.daycareFixture.id
      )
    ])
  })
  .afterEach(logConsoleMessages)

test('List of voucher value decision drafts shows at least one row', async (t) => {
  await employeeLogin(t, seppoAdmin, page.url)
  await page.navigateToValueDecisions(t)
  await t.click(page.areaFilter(fixtures.careAreaFixture.shortName))

  await t.expect(page.valueDecisionRows.count).gt(0)
})

test('Navigate to and from decision details page', async (t) => {
  await employeeLogin(t, seppoAdmin, page.url)
  await page.navigateToValueDecisions(t)
  await t.click(page.areaFilter(fixtures.careAreaFixture.shortName))

  await page.openFirstValueDecision(t)
  await t.expect(page.valueDecisionPage.exists).ok()

  await t.click(page.navigateBack)
  await t.expect(page.valueDecisionsPage.exists).ok()
})

test('voucher value decisions are toggled and sent', async (t) => {
  await employeeLogin(t, seppoAdmin, page.url)
  await page.navigateToValueDecisions(t)
  await t.click(page.areaFilter(fixtures.careAreaFixture.shortName))

  await t.expect(page.toggleAllValueDecisions.exists).ok()
  await t.expect(page.toggleAllValueDecisions.checked).eql(false)

  await page.toggleAllValueDecisions.click()
  await t.expect(page.toggleAllValueDecisions.checked).eql(true)

  await t.click(page.sendValueDecisions)
  await t
    .expect(page.sendValueDecisions.getAttribute('data-status'))
    .eql('success')
  await runPendingAsyncJobs()

  // wait until draft has disappeared and ui is stable
  await t.expect(page.valueDecisionRows.count).eql(0)

  await page.valueDecisionsStatusFilterSent.click()
  await t.expect(page.valueDecisionRows.count).eql(1)
})
