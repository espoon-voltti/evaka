// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  enduserChildFixtureJari,
  enduserGuardianFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  createIncomeStatements,
  resetServiceState
} from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  FinancePage,
  IncomeStatementsPage
} from '../../pages/employee/finance/finance-page'
import { waitUntilFalse, waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let nav: EmployeeNav
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0, 0)
const today = now.toLocalDate()

beforeEach(async () => {
  await resetServiceState()
  await initializeAreaAndPersonData()

  page = await Page.open({
    acceptDownloads: true,
    mockedTime: now
  })

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
    await createIncomeStatements({
      body: {
        personId: enduserGuardianFixture.id,
        data: [
          {
            type: 'HIGHEST_FEE',
            startDate: today.addYears(-1),
            endDate: today.addDays(-1)
          },
          {
            type: 'HIGHEST_FEE',
            startDate: today,
            endDate: null
          }
        ]
      }
    })

    let incomeStatementsPage = await navigateToIncomeStatements()
    await incomeStatementsPage.incomeStatementRows.assertCount(2)
    const personProfilePage =
      await incomeStatementsPage.openNthIncomeStatementForGuardian(1)

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
      (await incomesSection.getIncomeStatementInnerText(0)).includes(
        'this is a note'
      )
    )

    incomeStatementsPage = await navigateToIncomeStatements()
    await incomeStatementsPage.incomeStatementRows.assertCount(1)
  })

  test('Income statement can be filtered by child placement unit provider type', async () => {
    await Fixture.fridgeChild()
      .with({
        headOfChild: enduserGuardianFixture.id,
        childId: enduserChildFixtureJari.id,
        startDate: today.addYears(-1),
        endDate: today.addYears(1)
      })
      .save()

    const startDate = today.addYears(-1)
    const endDate = today

    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          enduserChildFixtureJari.id,
          daycareFixture.id,
          startDate,
          endDate
        )
      ]
    })

    await createIncomeStatements({
      body: {
        personId: enduserGuardianFixture.id,
        data: [
          {
            type: 'HIGHEST_FEE',
            startDate,
            endDate
          }
        ]
      }
    })

    const incomeStatementsPage = await navigateToIncomeStatements()
    await incomeStatementsPage.waitUntilLoaded()
    // No filters -> is shown
    await incomeStatementsPage.incomeStatementRows.assertCount(1)

    // Filter by the placed unit provider type -> is shown
    await incomeStatementsPage.selectProviderType(daycareFixture.providerType)
    await incomeStatementsPage.waitUntilLoaded()
    await incomeStatementsPage.incomeStatementRows.assertCount(1)

    // Filter by other unit provider type -> not shown
    await incomeStatementsPage.unSelectProviderType(daycareFixture.providerType)
    await incomeStatementsPage.selectProviderType('EXTERNAL_PURCHASED')
    await incomeStatementsPage.waitUntilLoaded()
    await incomeStatementsPage.incomeStatementRows.assertCount(0)
  })

  test('Child income statement is listed on finance worker unhandled income statement list', async () => {
    await createIncomeStatements({
      body: {
        personId: enduserChildFixtureJari.id,
        data: [
          {
            type: 'CHILD_INCOME',
            otherInfo: 'Test info',
            startDate: today,
            endDate: today,
            attachmentIds: []
          }
        ]
      }
    })

    const incomeStatementsPage = await navigateToIncomeStatements()
    await incomeStatementsPage.incomeStatementRows.assertCount(1)
    await incomeStatementsPage.assertNthIncomeStatement(
      0,
      `${enduserChildFixtureJari.lastName} ${enduserChildFixtureJari.firstName}`,
      'lapsen tulotiedot'
    )

    const profilePage =
      await incomeStatementsPage.openNthIncomeStatementForChild(0)
    const incomeSection = await profilePage.openCollapsible('income')
    await incomeSection.assertIncomeStatementRowCount(1)
    const incomeStatementPage = await incomeSection.openIncomeStatement(0)
    await incomeStatementPage.assertChildIncomeStatement('Test info', 0)
  })
})
