// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InvoicingPage from '../../pages/invoicing'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  daycareFixture,
  voucherValueDecisionsFixture
} from '../../dev-api/fixtures'
import {
  cleanUpInvoicingDatabase,
  insertVoucherValueDecisionFixtures
} from '../../dev-api'
import { logConsoleMessages } from '../../utils/fixture'
import { seppoAdminRole } from '../../config/users'
import ReportsPage from '../../pages/reports'

const page = new InvoicingPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Reporting - voucher reports')
  .meta({ type: 'regression', subType: 'reports' })
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
        fixtures.daycareFixture.id,
        'SENT'
      )
    ])
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUpInvoicingDatabase()
    await cleanUp()
  })

const reports = new ReportsPage()

test('voucher service providers are reported correctly', async (t) => {
  await t.useRole(seppoAdminRole)
  await reports.selectReportsTab()
  await reports.selectVoucherServiceProvidersReport()

  await reports.selectMonth('Tammikuu')
  await reports.selectYear(2020)
  await reports.selectArea('Superkeskus')

  await reports.assertVoucherServiceProviderRow(daycareFixture.id, '1', '-289')
})
