// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
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
  await resetServiceState()
  const area = await Fixture.careArea().save()
  const unit = await Fixture.daycare({
    areaId: area.id,
    businessId: 'businessId',
    providerId: 'providerId',
    iban: 'iban'
  }).save()

  await Fixture.payment({
    unitId: unit.id,
    unitName: unit.name,
    amount: 10000
  }).save()

  page = await Page.open({})

  const financeAdmin = await Fixture.employee().financeAdmin().save()
  await employeeLogin(page, financeAdmin)

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

    await paymentsPage.confirmPayments()
    await paymentsPage.assertPaymentCount(0)

    await paymentsPage.setStatusFilter('CONFIRMED')
    await paymentsPage.assertPaymentCount(1)

    await paymentsPage.togglePayments(true)
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

  test('are reverted', async () => {
    await paymentsPage.togglePayments(true)
    await paymentsPage.assertPaymentCount(1)

    await paymentsPage.confirmPayments()
    await paymentsPage.assertPaymentCount(0)

    await paymentsPage.setStatusFilter('CONFIRMED')
    await paymentsPage.assertPaymentCount(1)

    await paymentsPage.togglePayments(true)
    await paymentsPage.revertPayments()
    await paymentsPage.assertPaymentCount(0)

    await paymentsPage.setStatusFilter('DRAFT')
    await paymentsPage.assertPaymentCount(1)
  })
})
