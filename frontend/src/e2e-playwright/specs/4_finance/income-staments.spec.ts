// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import {
  FinancePage,
  IncomeStatementsPage
} from 'e2e-playwright/pages/employee/finance/finance-page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertEmployeeFixture,
  insertIncomeStatements,
  insertPersonFixture,
  resetDatabase
} from 'e2e-test-common/dev-api'
import LocalDate from 'lib-common/local-date'
import { Page } from 'playwright'
import { enduserGuardianFixture } from '../../../e2e-test-common/dev-api/fixtures'
import { newBrowserContext } from '../../browser'
import { PersonProfilePage } from '../../pages/employee/person-profile'

let page: Page
let nav: EmployeeNav
let financePage: FinancePage
let incomeStatementsPage: IncomeStatementsPage
let personProfilePage: PersonProfilePage

beforeEach(async () => {
  await resetDatabase()
  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()

  await insertPersonFixture(enduserGuardianFixture)
  await insertIncomeStatements(enduserGuardianFixture.id, [
    {
      type: 'HIGHEST_FEE',
      startDate: LocalDate.today().addYears(-1),
      endDate: LocalDate.today().addDays(-1)
    },
    {
      type: 'HIGHEST_FEE',
      startDate: LocalDate.today(),
      endDate: null
    }
  ])

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
  nav = new EmployeeNav(page)
  financePage = new FinancePage(page)
  incomeStatementsPage = new IncomeStatementsPage(page)
  personProfilePage = new PersonProfilePage(page)

  await navigateToIncomeStatements()
})
afterEach(async () => {
  await page.close()
})

async function navigateToIncomeStatements() {
  await nav.openTab('finance')
  await financePage.selectIncomeStatementsTab()
}

describe('Income statements', () => {
  test('Income statement can be set handled', async () => {
    await waitUntilEqual(() => incomeStatementsPage.getRowCount(), 2)
    await incomeStatementsPage.openNthIncomeStatement(1)
    await personProfilePage.openCollapsible('person-income')
    await personProfilePage.setIncomeStatementHandled()
    await navigateToIncomeStatements()
    await waitUntilEqual(() => incomeStatementsPage.getRowCount(), 1)
  })
})
