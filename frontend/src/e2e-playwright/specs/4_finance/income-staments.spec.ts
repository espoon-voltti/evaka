// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import {
  FinancePage,
  IncomeStatementsPage
} from 'e2e-playwright/pages/employee/finance/finance-page'
import {
  waitUntilEqual,
  waitUntilFalse,
  waitUntilTrue
} from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertIncomeStatements,
  insertPersonFixture,
  resetDatabase
} from 'e2e-test-common/dev-api'
import {
  enduserGuardianFixture,
  Fixture
} from 'e2e-test-common/dev-api/fixtures'
import LocalDate from 'lib-common/local-date'
import { Page } from '../../utils/page'

let page: Page
let nav: EmployeeNav

beforeEach(async () => {
  await resetDatabase()
  page = await Page.open({ acceptDownloads: true })

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

  const financeAdmin = await Fixture.employeeFinanceAdmin().save()
  await employeeLogin(page, financeAdmin.data)

  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
})

async function navigateToIncomeStatements() {
  await nav.openTab('finance')
  await new FinancePage(page).selectIncomeStatementsTab()
  return new IncomeStatementsPage(page)
}

describe('Income statements', () => {
  test('Income statement can be set handled', async () => {
    let incomeStatementsPage = await navigateToIncomeStatements()
    await waitUntilEqual(() => incomeStatementsPage.getRowCount(), 2)
    const personProfilePage = await incomeStatementsPage.openNthIncomeStatement(
      1
    )

    const incomesSection = await personProfilePage.openCollapsible('incomes')
    await waitUntilFalse(() => incomesSection.isIncomeStatementHandled(0))
    await waitUntilFalse(() => incomesSection.isIncomeStatementHandled(1))

    const incomeStatementPage = await incomesSection.openIncomeStatement(0)

    await incomeStatementPage.typeHandlerNote('this is a note')
    await incomeStatementPage.setHandled(true)
    await incomeStatementPage.submit()

    await waitUntilTrue(() => incomesSection.isIncomeStatementHandled(0))
    await waitUntilFalse(() => incomesSection.isIncomeStatementHandled(1))

    await waitUntilTrue(async () =>
      (
        await incomesSection.getIncomeStatementInnerText(0)
      ).includes('this is a note')
    )

    incomeStatementsPage = await navigateToIncomeStatements()
    await waitUntilEqual(() => incomeStatementsPage.getRowCount(), 1)
  })
})
