// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InvoicingPage from '../../pages/invoicing'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { voucherValueDecisionsFixture } from '../../dev-api/fixtures'
import {
  cleanUpInvoicingDatabase,
  insertVoucherValueDecisionFixtures,
  runPendingAsyncJobs
} from '../../dev-api'
import { logConsoleMessages } from '../../utils/fixture'
import { seppoAdminRole } from '../../config/users'

const page = new InvoicingPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Invoicing - voucher value decisions')
  .meta({ type: 'regression', subType: 'voucherValueDecisions' })
  .page(page.url)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .beforeEach(async () => {
    await cleanUpInvoicingDatabase()
    await insertVoucherValueDecisionFixtures([
      voucherValueDecisionsFixture(
        fixtures.enduserGuardianFixture.id,
        fixtures.enduserChildFixtureKaarina.id,
        fixtures.daycareFixture.id
      )
    ])
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUpInvoicingDatabase()
    await cleanUp()
  })

test('List of voucher value decision drafts shows at least one row', async (t) => {
  await t.useRole(seppoAdminRole)
  await page.navigateToValueDecisions(t)
  await t.click(page.areaFilter(fixtures.careAreaFixture.shortName))

  await t.expect(page.valueDecisionRows.count).gt(0)
})

test('Navigate to and from decision details page', async (t) => {
  await t.useRole(seppoAdminRole)
  await page.navigateToValueDecisions(t)
  await t.click(page.areaFilter(fixtures.careAreaFixture.shortName))

  await page.openFirstValueDecision(t)
  await t.expect(page.valueDecisionPage.exists).ok()

  await t.click(page.navigateBack)
  await t.expect(page.valueDecisionsPage.exists).ok()
})

test('voucher value decisions are toggled and sent', async (t) => {
  await t.useRole(seppoAdminRole)
  await page.navigateToValueDecisions(t)
  await t.click(page.areaFilter(fixtures.careAreaFixture.shortName))

  await t.expect(page.toggleAllValueDecisions.exists).ok()
  await t.expect(page.toggleAllValueDecisions.checked).eql(false)

  await page.toggleAllValueDecisions.click()
  await t.expect(page.toggleAllValueDecisions.checked).eql(true)

  await t.click(page.sendValueDecisions)
  await runPendingAsyncJobs()

  await page.valueDecisionsStatusFilterSent.click()
  await t.expect(page.valueDecisionRows.count).eql(1)
})
