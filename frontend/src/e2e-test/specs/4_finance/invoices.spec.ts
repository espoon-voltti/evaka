// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevPlacement } from 'e2e-test/generated/api-types'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  familyWithRestrictedDetailsGuardian,
  feeDecisionsFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createFeeDecisions,
  deletePlacement,
  generateReplacementDraftInvoices,
  resetServiceState
} from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  FinancePage,
  InvoicesPage
} from '../../pages/employee/finance/finance-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page

const now = HelsinkiDateTime.of(2024, 11, 1, 12, 0)
const today = now.toLocalDate()

const codebtor = Fixture.person({
  firstName: 'Code',
  lastName: 'Btor',
  ssn: '010177-1234'
}).data

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()

  await Fixture.family({
    guardian: testAdult,
    otherGuardian: codebtor,
    children: [testChild, testChild2]
  }).save()
  await Fixture.guardian(testChild, testAdult).save()
  await Fixture.guardian(testChild2, testAdult).save()
  await Fixture.guardian(testChild, codebtor).save()
  await Fixture.guardian(testChild2, codebtor).save()

  await Fixture.parentship({
    childId: testChild.id,
    headOfChildId: testAdult.id,
    startDate: testChild.dateOfBirth,
    endDate: testChild.dateOfBirth.addYears(18).subDays(1)
  }).save()
  await Fixture.parentship({
    childId: testChild2.id,
    headOfChildId: testAdult.id,
    startDate: testChild2.dateOfBirth,
    endDate: testChild2.dateOfBirth.addYears(18).subDays(1)
  }).save()

  await Fixture.family(familyWithRestrictedDetailsGuardian).save()

  await Fixture.feeThresholds().save()
})

async function openInvoicesPage(): Promise<InvoicesPage> {
  page = await Page.open({ acceptDownloads: true, mockedTime: now })

  const financeAdmin = await Fixture.employee().financeAdmin().save()
  await employeeLogin(page, financeAdmin)

  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')
  const financePage = new FinancePage(page)
  const invoicesPage = await financePage.selectInvoicesTab()

  return invoicesPage
}

describe('Invoices', () => {
  describe('Create drafts', () => {
    const feeDecision = feeDecisionsFixture(
      'SENT',
      testAdult,
      testChild2,
      testDaycare.id,
      codebtor,
      FiniteDateRange.ofMonth(today.subMonths(1)),
      now,
      'bcc42d48-765d-4fe1-bc90-7a7b4c8205fe',
      null,
      123123123
    )

    beforeEach(async () => {
      await createFeeDecisions({ body: [feeDecision] })
      await Fixture.placement({
        childId: testChild2.id,
        unitId: testDaycare.id,
        startDate: feeDecision.validDuring.start,
        endDate: feeDecision.validDuring.end
      }).save()
    })

    test('List of invoice drafts is empty intially and after creating new drafts the list has one invoice', async () => {
      const invoicesPage = await openInvoicesPage()
      await invoicesPage.assertInvoiceCount(0)
      await invoicesPage.createInvoiceDrafts()
      await invoicesPage.assertInvoiceCount(1)
    })

    test('Invoice page has correct content', async () => {
      const invoicesPage = await openInvoicesPage()
      await invoicesPage.createInvoiceDrafts()
      const invoicePage = await invoicesPage.openFirstInvoice()

      const head = invoicePage.headOfFamilySection
      await head.headOfFamilyName.assertTextEquals(
        `${testAdult.firstName} ${testAdult.lastName}`
      )
      await head.headOfFamilySsn.assertTextEquals(testAdult.ssn!)
      await head.codebtorName.assertTextEquals(
        `${codebtor.firstName} ${codebtor.lastName}`
      )
      await head.codebtorSsn.assertTextEquals(codebtor.ssn!)

      const details = invoicePage.detailsSection
      await details.status.assertTextEquals('Luonnos')

      const periodStart = today.subMonths(1).withDate(1)
      const periodEnd = today.withDate(1).subDays(1)
      await details.period.assertTextEquals(
        `${periodStart.format()} - ${periodEnd.format()}`
      )

      await details.number.assertTextEquals('')
      await details.dueDate.assertTextEquals(today.addDays(28).format())
      await details.account.assertTextEquals('3295')
      await details.agreementType.assertTextEquals('299')
      await details.relatedFeeDecisions.assertTextEquals(
        feeDecision.decisionNumber!.toString()
      )
      await details.replacedInvoice.waitUntilHidden()

      const child = invoicePage.nthChild(0)
      await child.childName.assertTextEquals(
        `${testChild2.lastName} ${testChild2.firstName}`
      )
      await child.childSsn.assertTextEquals(testChild2.ssn!)

      const row = child.row(0)
      await row.product.assertTextEquals('Varhaiskasvatus')
      await row.description.assertTextEquals('')
      await row.unit.assertTextEquals(testDaycare.name)
      await row.period.assertTextEquals(
        `${periodStart.format()} - ${periodEnd.format()}`
      )
      await row.amount.assertTextEquals('1')
      await row.unitPrice.assertTextEquals('289')
      await row.totalPrice.assertTextEquals('289')

      await child.totalPrice.assertTextEquals('289')
      await child.previousTotalPrice.waitUntilHidden()

      await invoicePage.totalPrice.assertTextEquals('289')
      await invoicePage.previousTotalPrice.waitUntilHidden()

      await invoicesPage.navigateBackToInvoices()
    })
  })

  describe('Send invoices', () => {
    test('Invoices are toggled and sent', async () => {
      await Fixture.invoice({
        headOfFamilyId: testAdult.id,
        areaId: testCareArea.id
      })
        .addRow({
          childId: testChild.id,
          unitId: testDaycare.id
        })
        .save()
      await Fixture.invoice({
        headOfFamilyId: familyWithRestrictedDetailsGuardian.guardian.id,
        areaId: testCareArea.id
      })
        .addRow({
          childId: familyWithRestrictedDetailsGuardian.children[0].id,
          unitId: testDaycare.id
        })
        .save()
      const invoicesPage = await openInvoicesPage()

      await invoicesPage.toggleAllInvoices(true)
      await invoicesPage.assertInvoiceCount(2)
      await invoicesPage.sendInvoices()
      await invoicesPage.assertInvoiceCount(0)
      await invoicesPage.filterByStatus('SENT')
      await invoicesPage.assertInvoiceCount(2)
    })

    test('Sending an invoice with a recipient without a SSN', async () => {
      const adultWithoutSSN = await Fixture.person({
        id: 'a6cf0ec0-4573-4816-be30-6b87fd943817',
        firstName: 'Aikuinen',
        lastName: 'Hetuton',
        ssn: null,
        dateOfBirth: LocalDate.of(1980, 1, 1),
        streetAddress: 'Kamreerintie 2',
        postalCode: '02770',
        postOffice: 'Espoo'
      }).saveAdult()

      await Fixture.invoice({
        headOfFamilyId: adultWithoutSSN.id,
        areaId: testCareArea.id
      })
        .addRow({ childId: testChild.id, unitId: testDaycare.id })
        .save()

      const invoicesPage = await openInvoicesPage()
      await invoicesPage.freeTextFilter(adultWithoutSSN.firstName)
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.toggleAllInvoices(true)
      await invoicesPage.sendInvoices()
      await invoicesPage.assertInvoiceCount(0)
      await invoicesPage.filterByStatus('WAITING_FOR_SENDING')
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.openFirstInvoice()
      await invoicesPage.markInvoiceSent()
      await invoicesPage.navigateBackToInvoices()
      await invoicesPage.filterByStatus('SENT')
      await invoicesPage.assertInvoiceCount(1)
    })
  })

  describe('Replacement invoices', () => {
    const feeDecision = feeDecisionsFixture(
      'SENT',
      testAdult,
      testChild2,
      testDaycare.id,
      codebtor,
      FiniteDateRange.ofMonth(today.subMonths(1)),
      now,
      'bcc42d48-765d-4fe1-bc90-7a7b4c8205fe',
      null,
      123123123
    )
    let placement: DevPlacement
    let invoicesPage: InvoicesPage

    beforeEach(async () => {
      await createFeeDecisions({ body: [feeDecision] })
      placement = await Fixture.placement({
        childId: testChild2.id,
        unitId: testDaycare.id,
        startDate: feeDecision.validDuring.start,
        endDate: feeDecision.validDuring.end
      }).save()

      invoicesPage = await openInvoicesPage()

      await invoicesPage.createInvoiceDrafts()
      await invoicesPage.toggleAllInvoices(true)
      await invoicesPage.sendInvoices()
    })

    test('Replacement invoice content', async () => {
      // Add an absence => replacement invoice is generated
      await Fixture.absence({
        childId: testChild2.id,
        date: today.subMonths(1).withDate(1),
        absenceType: 'FORCE_MAJEURE',
        absenceCategory: 'BILLABLE'
      }).save()
      await generateReplacementDraftInvoices()

      await invoicesPage.filterByStatus('REPLACEMENT_DRAFT')

      const invoicePage = await invoicesPage.openFirstInvoice()

      const details = invoicePage.detailsSection
      await details.status.assertTextEquals('Oikaisuluonnos')

      await details.relatedFeeDecisions.assertTextEquals(
        feeDecision.decisionNumber!.toString()
      )
      await details.replacedInvoice.assertTextEquals('Lasku 10/2024')

      const child = invoicePage.nthChild(0)
      await child.totalPrice.assertTextEquals('276,43')
      await child.previousTotalPrice.assertTextEquals('289')

      await invoicePage.totalPrice.assertTextEquals('276,43')
      await invoicePage.previousTotalPrice.assertTextEquals('289')

      await invoicesPage.navigateBackToInvoices()
    })

    test('Replacement invoice content when there are no rows', async () => {
      // Delete the placement => replacement invoice with no rows is generated
      await deletePlacement({ placementId: placement.id })
      await generateReplacementDraftInvoices()

      await invoicesPage.filterByStatus('REPLACEMENT_DRAFT')

      const invoicePage = await invoicesPage.openFirstInvoice()

      const details = invoicePage.detailsSection
      await details.status.assertTextEquals('Oikaisuluonnos')

      await details.replacedInvoice.assertTextEquals('Lasku 10/2024')

      const child = invoicePage.nthChild(0)
      await child.childName.assertTextEquals(
        `${testChild2.lastName} ${testChild2.firstName}`
      )
      await child.childSsn.assertTextEquals(testChild2.ssn!)
      await child.totalPrice.assertTextEquals('0')
      await child.previousTotalPrice.assertTextEquals('289')

      await invoicePage.totalPrice.assertTextEquals('0')
      await invoicePage.previousTotalPrice.assertTextEquals('289')

      await invoicesPage.navigateBackToInvoices()
    })
  })
})
