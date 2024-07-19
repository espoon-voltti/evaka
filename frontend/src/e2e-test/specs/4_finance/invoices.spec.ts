// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'

import { startTest } from '../../browser'
import config from '../../config'
import {
  createDaycarePlacementFixture,
  familyWithRestrictedDetailsGuardian,
  feeDecisionsFixture,
  Fixture,
  invoiceFixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  createFeeDecisions,
  createInvoices
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  FinancePage,
  InvoicesPage
} from '../../pages/employee/finance/finance-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let financePage: FinancePage
let invoicesPage: InvoicesPage
let feeDecisionFixture: FeeDecision
let adultWithoutSSN: DevPerson

beforeEach(async () => {
  await startTest()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await Fixture.family(familyWithRestrictedDetailsGuardian).save()
  adultWithoutSSN = await Fixture.person({
    id: 'a6cf0ec0-4573-4816-be30-6b87fd943817',
    firstName: 'Aikuinen',
    lastName: 'Hetuton',
    ssn: null,
    dateOfBirth: LocalDate.of(1980, 1, 1),
    streetAddress: 'Kamreerintie 2',
    postalCode: '02770',
    postOffice: 'Espoo'
  }).saveAdult()
  await Fixture.parentship({
    childId: testChild2.id,
    headOfChildId: testAdult.id,
    startDate: testChild2.dateOfBirth,
    endDate: LocalDate.of(2099, 1, 1)
  }).save()

  feeDecisionFixture = feeDecisionsFixture(
    'SENT',
    testAdult,
    testChild2,
    testDaycare.id,
    null,
    new DateRange(
      LocalDate.todayInSystemTz().subMonths(1).withDate(1),
      LocalDate.todayInSystemTz().withDate(1).subDays(1)
    )
  )
  await createFeeDecisions({ body: [feeDecisionFixture] })
  await createDaycarePlacements({
    body: [
      createDaycarePlacementFixture(
        uuidv4(),
        testChild2.id,
        testDaycare.id,
        feeDecisionFixture.validDuring.start,
        feeDecisionFixture.validDuring.end ?? undefined
      )
    ]
  })

  await Fixture.feeThresholds().save()

  page = await Page.open({ acceptDownloads: true })

  const financeAdmin = await Fixture.employee().financeAdmin().save()
  await employeeLogin(page, financeAdmin)

  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')
  financePage = new FinancePage(page)
  invoicesPage = await financePage.selectInvoicesTab()
})

describe('Invoices', () => {
  test('List of invoice drafts is empty intially and after creating new drafts the list has one invoice', async () => {
    await invoicesPage.assertInvoiceCount(0)
    await invoicesPage.createInvoiceDrafts()
    await invoicesPage.assertInvoiceCount(1)
  })

  test('Navigate to and from invoice page', async () => {
    await invoicesPage.createInvoiceDrafts()
    await invoicesPage.openFirstInvoice()
    await invoicesPage.assertInvoiceHeadOfFamily(
      `${testAdult.firstName} ${testAdult.lastName}`
    )
    await invoicesPage.navigateBackToInvoices()
  })

  test('Add a new invoice row, modify its amount and price and persist the changes', async () => {
    await invoicesPage.createInvoiceDrafts()
    await invoicesPage.openFirstInvoice()
    await invoicesPage.assertInvoiceRowCount(1)
    await invoicesPage.addNewInvoiceRow(
      'DAYCARE_INCREASE',
      testDaycare.name,
      10,
      100
    )
    await invoicesPage.navigateBackToInvoices()
    const originalInvoiceTotal = feeDecisionFixture.children.reduce(
      (sum, { finalFee }) => sum + finalFee / 100,
      0
    )
    await invoicesPage.assertInvoiceTotal(originalInvoiceTotal + 10 * 100)
  })

  test('Delete an invoice row', async () => {
    await invoicesPage.createInvoiceDrafts()
    await invoicesPage.openFirstInvoice()
    await invoicesPage.assertInvoiceRowCount(1)
    await invoicesPage.deleteInvoiceRow(0)
    await invoicesPage.assertInvoiceRowCount(0)
  })

  test('Invoices are toggled and sent', async () => {
    await createInvoices({
      body: [
        invoiceFixture(
          testAdult.id,
          testChild.id,
          testCareArea.id,
          testDaycare.id,
          'DRAFT'
        ),
        invoiceFixture(
          familyWithRestrictedDetailsGuardian.guardian.id,
          familyWithRestrictedDetailsGuardian.children[0].id,
          testCareArea.id,
          testDaycare.id,
          'DRAFT'
        )
      ]
    })
    // switch tabs to refresh data
    await financePage.selectFeeDecisionsTab()
    await financePage.selectInvoicesTab()

    await invoicesPage.toggleAllInvoices(true)
    await invoicesPage.assertInvoiceCount(2)
    await invoicesPage.sendInvoices()
    await invoicesPage.assertInvoiceCount(0)
    await invoicesPage.showSentInvoices()
    await invoicesPage.assertInvoiceCount(2)
  })

  test('Sending an invoice with a recipient without a SSN', async () => {
    await createInvoices({
      body: [
        invoiceFixture(
          adultWithoutSSN.id,
          testChild.id,
          testCareArea.id,
          testDaycare.id,
          'DRAFT'
        )
      ]
    })

    await invoicesPage.freeTextFilter(adultWithoutSSN.firstName)
    await invoicesPage.assertInvoiceCount(1)
    await invoicesPage.toggleAllInvoices(true)
    await invoicesPage.sendInvoices()
    await invoicesPage.assertInvoiceCount(0)
    await invoicesPage.showWaitingForSendingInvoices()
    await invoicesPage.assertInvoiceCount(1)
    await invoicesPage.openFirstInvoice()
    await invoicesPage.markInvoiceSent()
    await invoicesPage.navigateBackToInvoices()
    await invoicesPage.showSentInvoices()
    await invoicesPage.assertInvoiceCount(1)
  })
})
