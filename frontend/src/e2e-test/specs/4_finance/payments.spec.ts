// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  FinancePage,
  PaymentsPage
} from '../../pages/employee/finance/finance-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let financePage: FinancePage
let paymentsPage: PaymentsPage

beforeEach(async () => {
  await resetDatabase()
  const { data: unit } = await Fixture.daycare()
    .careArea(await Fixture.careArea().save())
    .with({
      businessId: 'businessId',
      providerId: 'providerId',
      iban: 'iban'
    })
    .save()

  await Fixture.payment().with({ unitId: unit.id, amount: 10000 }).save()

  page = await Page.open({})

  const financeAdmin = await Fixture.employeeFinanceAdmin().save()
  await employeeLogin(page, financeAdmin.data)

  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')
  financePage = new FinancePage(page)
  paymentsPage = await financePage.selectPaymentsTab()
})

describe('Payments', () => {
  test('are toggled and sent', async () => {
    await paymentsPage.togglePayments(true)
    await paymentsPage.assertPaymentCount(1)
    await paymentsPage.sendPayments()
    await paymentsPage.assertPaymentCount(0)
    await paymentsPage.setStatusFilter('SENT')
    await paymentsPage.assertPaymentCount(1)
  })

  test('are deleted', async () => {
    await paymentsPage.togglePayments(true)
    await paymentsPage.assertPaymentCount(1)
    await paymentsPage.deletePayments()
    await paymentsPage.assertPaymentCount(0)
  })
})
