// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'testcafe'
import InvoicingPage from '../../pages/invoicing'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { feeDecisionsFixture } from 'e2e-test-common/dev-api/fixtures'
import {
  cleanUpInvoicingDatabase,
  insertFeeDecisionFixtures,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import { logConsoleMessages } from '../../utils/fixture'
import { seppoAdminRole } from '../../config/users'

const page = new InvoicingPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Invoicing - fee decisions')
  .meta({ type: 'regression', subType: 'feeDecisions' })
  .page(page.url)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .beforeEach(async (t) => {
    await cleanUpInvoicingDatabase()
    await insertFeeDecisionFixtures([
      feeDecisionsFixture(
        'DRAFT',
        fixtures.enduserGuardianFixture,
        fixtures.enduserChildFixtureKaarina,
        fixtures.daycareFixture.id
      )
    ])

    await t.useRole(seppoAdminRole)
    await page.navigateToFeeDecisions(t)
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUpInvoicingDatabase()
    await cleanUp()
  })

test('List of fee decision drafts shows at least one row', async (t) => {
  await t.expect(page.feeDecisionTable.visible).ok()
  await t.expect(page.feeDecisionRows.count).gt(0)
})

test('Navigate to and from decision details page', async (t) => {
  await page.openFirstFeeDecision(t)
  await t.expect(page.feeDecisionDetailsPage.exists).ok()

  await t.expect(page.navigateBack.exists).ok()
  await t.click(page.navigateBack)
  await t.expect(page.feeDecisionsPage.exists).ok()
})

test('Fee decisions are toggled and confirmed', async (t) => {
  await t.expect(page.toggleAllFeeDecisions.exists).ok()
  await t.expect(page.toggleFirstFeeDecision.exists).ok()
  await t.expect(page.toggleAllFeeDecisions.checked).eql(false)
  await t.expect(page.toggleFirstFeeDecision.checked).eql(false)

  await page.toggleAllFeeDecisions.click()
  await t.expect(page.toggleAllFeeDecisions.checked).eql(true)
  await t.expect(page.toggleFirstFeeDecision.checked).eql(true)

  await page.toggleAllFeeDecisions.click()
  await t.expect(page.toggleAllFeeDecisions.checked).eql(false)
  await t.expect(page.toggleFirstFeeDecision.checked).eql(false)

  await page.toggleFirstFeeDecision.click()
  await t.expect(page.toggleFirstFeeDecision.checked).eql(true)

  await page.toggleFirstFeeDecision.click()
  await t.expect(page.toggleFirstFeeDecision.checked).eql(false)

  await page.toggleFirstFeeDecision.click()
  await t.click(page.confirmFeeDecisions)
  await runPendingAsyncJobs()

  await page.feeDecisionsStatusFilterDraft.click()
  await page.feeDecisionsStatusFilterSent.click()
  await t.expect(page.feeDecisionRows.count).eql(1)
})
