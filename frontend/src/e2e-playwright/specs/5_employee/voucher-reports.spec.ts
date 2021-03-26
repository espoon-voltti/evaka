// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careArea2Fixture,
  daycare2Fixture,
  daycareFixture,
  Fixture,
  voucherValueDecisionsFixture
} from 'e2e-test-common/dev-api/fixtures'
import {
  cleanUpInvoicingDatabase,
  insertVoucherValueDecisionFixtures
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import ReportsPage from 'e2e-playwright/pages/employee/reports'
import assert from 'assert'

let fixtures: AreaAndPersonFixtures

let page: Page
let reports: ReportsPage
beforeAll(async () => {
  ;[fixtures] = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
})
beforeEach(async () => {
  await cleanUpInvoicingDatabase()
  await insertVoucherValueDecisionFixtures([
    voucherValueDecisionsFixture(
      fixtures.enduserGuardianFixture.id,
      fixtures.enduserChildFixtureKaarina.id,
      fixtures.daycareFixture.id,
      'SENT',
      '2020-01-01',
      '2020-12-31'
    ),
    voucherValueDecisionsFixture(
      fixtures.enduserGuardianFixture.id,
      fixtures.enduserChildFixtureJari.id,
      daycare2Fixture.id,
      'SENT',
      '2020-01-01',
      '2020-12-31'
    )
  ])

  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()
  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.login('admin')
  await nav.openTab('reports')

  reports = new ReportsPage(page)
})
afterEach(async () => {
  await page.close()
})
afterAll(async () => {
  await cleanUpInvoicingDatabase()
  await Fixture.cleanup()
})

describe('Reporting - voucher reports', () => {
  test('voucher service providers are reported correctly, respecting the area filter', async () => {
    await reports.selectVoucherServiceProvidersReport()

    await reports.selectMonth('Tammikuu')
    await reports.selectYear(2020)
    await reports.selectArea('Superkeskus')

    await reports.assertVoucherServiceProviderRowCount(1)
    await reports.assertVoucherServiceProviderRow(
      daycareFixture.id,
      '1',
      '-289,00'
    )

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
})
