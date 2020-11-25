// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InvoicingPage from '../../pages/invoicing'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  careArea2Fixture,
  daycare2Fixture,
  daycareFixture,
  Fixture,
  voucherValueDecisionsFixture
} from '../../dev-api/fixtures'
import {
  cleanUpInvoicingDatabase,
  insertVoucherValueDecisionFixtures
} from '../../dev-api'
import { logConsoleMessages } from '../../utils/fixture'
import { seppoAdminRole } from '../../config/users'
import ReportsPage from '../../pages/reports'
import assert from 'assert'

const page = new InvoicingPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Reporting - voucher reports')
  .meta({ type: 'regression', subType: 'reports' })
  .page(page.url)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    const careArea = await Fixture.careArea().with(careArea2Fixture).save()
    await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
  })
  .beforeEach(async () => {
    await cleanUpInvoicingDatabase()
    await insertVoucherValueDecisionFixtures([
      voucherValueDecisionsFixture(
        fixtures.enduserGuardianFixture.id,
        fixtures.enduserChildFixtureKaarina.id,
        fixtures.daycareFixture.id,
        'SENT'
      ),
      voucherValueDecisionsFixture(
        fixtures.enduserGuardianFixture.id,
        fixtures.enduserChildFixtureJari.id,
        daycare2Fixture.id,
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

test('voucher service providers are reported correctly, respecting the area filter', async (t) => {
  await t.useRole(seppoAdminRole)
  await reports.selectReportsTab()
  await reports.selectVoucherServiceProvidersReport()

  await reports.selectMonth('Tammikuu')
  await reports.selectYear(2020)
  await reports.selectArea('Superkeskus')

  await reports.assertVoucherServiceProviderRowCount(1)
  await reports.assertVoucherServiceProviderRow(daycareFixture.id, '1', '-289')

  const report = await reports.getCsvReport()
  assert(
    report.includes(daycareFixture.name),
    `Expected csv report to contain ${daycareFixture.name}`
  )
  assert(
    !report.includes(daycare2Fixture.name),
    `Expected csv report to not contain ${daycare2Fixture.name}`
  )
})
