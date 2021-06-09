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
  insertVoucherValueDecisionFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import InvoicingPage from 'e2e-playwright/pages/employee/invoicing/invoicing-page'
import LocalDate from 'lib-common/local-date'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'

let page: Page
let invoicingPage: InvoicingPage
const decision1DateFrom = LocalDate.today().addWeeks(1)
const decision1DateTo = LocalDate.today().addWeeks(2)
const decision2DateFrom = LocalDate.today().addWeeks(4)
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
  await employeeLogin(page, 'ADMIN')
  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')

  invoicingPage = new InvoicingPage(page)
})
afterEach(async () => {
  await page.close()
})

describe('Value decisions', () => {
  test('Date filter filters out decisions', async () => {
    await invoicingPage.selectTab('value-decisions')
    await invoicingPage.setDates(
      decision1DateFrom.subDays(1),
      decision2DateTo.addDays(1)
    )
    await waitUntilEqual(() => invoicingPage.getVoucherDecisionCount(), 2)

    await invoicingPage.setDates(
      decision1DateTo.addDays(1),
      decision2DateTo.addDays(1)
    )
    await waitUntilEqual(() => invoicingPage.getVoucherDecisionCount(), 1)
  })
  test('With two decisions any date filter overlap will show the decision', async () => {
    await invoicingPage.selectTab('value-decisions')
    await invoicingPage.setDates(
      decision1DateTo.subDays(1),
      decision2DateFrom.addDays(1)
    )
    await waitUntilEqual(() => invoicingPage.getVoucherDecisionCount(), 2)
  })
  test('Start date checkbox will filter out decisions that do not have a startdate within the date range', async () => {
    await invoicingPage.selectTab('value-decisions')
    await invoicingPage.setDates(
      decision1DateTo.subDays(1),
      decision2DateFrom.addDays(1)
    )
    await waitUntilEqual(() => invoicingPage.getVoucherDecisionCount(), 2)
    await invoicingPage.startDateWithinrange()
    await waitUntilEqual(() => invoicingPage.getVoucherDecisionCount(), 1)
  })
})
