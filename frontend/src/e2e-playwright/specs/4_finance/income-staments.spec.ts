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
import LocalDate from 'lib-common/local-date'
import {
  enduserGuardianFixture,
  Fixture
} from '../../../e2e-test-common/dev-api/fixtures'
import { IncomeStatementPage } from '../../pages/employee/IncomeStatementPage'
import { PersonProfilePage } from '../../pages/employee/person-profile'
import { Page } from '../../utils/page'

let page: Page
let nav: EmployeeNav
let financePage: FinancePage
let incomeStatementsPage: IncomeStatementsPage
let incomeStatementPage: IncomeStatementPage
let personProfilePage: PersonProfilePage

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

  await Fixture.employee()
    .with({
      id: config.financeAdminAad,
      externalId: `espoo-ad:${config.financeAdminAad}`,
      roles: ['FINANCE_ADMIN']
    })
    .save()
  await employeeLogin(page, 'FINANCE_ADMIN')

  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  financePage = new FinancePage(page)
  incomeStatementsPage = new IncomeStatementsPage(page)
  incomeStatementPage = new IncomeStatementPage(page)
  personProfilePage = new PersonProfilePage(page)
})

async function navigateToIncomeStatements() {
  await nav.openTab('finance')
  await financePage.selectIncomeStatementsTab()
}

describe('Income statements', () => {
  test('Income statement can be set handled', async () => {
    await navigateToIncomeStatements()
    await waitUntilEqual(() => incomeStatementsPage.getRowCount(), 2)
    await incomeStatementsPage.openNthIncomeStatement(1)
    await personProfilePage.waitUntilLoaded()
    await personProfilePage.openCollapsible('person-income')

    await waitUntilFalse(() => personProfilePage.isIncomeStatementHandled(0))
    await waitUntilFalse(() => personProfilePage.isIncomeStatementHandled(1))

    await personProfilePage.openIncomeStatement(0)

    await incomeStatementPage.typeHandlerNote('this is a note')
    await incomeStatementPage.setHandled(true)
    await incomeStatementPage.submit()

    await waitUntilTrue(() => personProfilePage.isIncomeStatementHandled(0))
    await waitUntilFalse(() => personProfilePage.isIncomeStatementHandled(1))

    await waitUntilTrue(async () =>
      (
        await personProfilePage.getIncomeStatementInnerText(0)
      ).includes('this is a note')
    )

    await navigateToIncomeStatements()
    await waitUntilEqual(() => incomeStatementsPage.getRowCount(), 1)
  })
})
