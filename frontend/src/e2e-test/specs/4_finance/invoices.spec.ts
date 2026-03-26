// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  FeeDecisionId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'
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
import type { DevEmployee, DevPlacement } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import type { InvoicesPage } from '../../pages/employee/finance/finance-page'
import { FinancePage } from '../../pages/employee/finance/finance-page'
import { expect, test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const now = HelsinkiDateTime.of(2024, 11, 1, 12, 0)
const today = now.toLocalDate()

const codebtor = Fixture.person({
  firstName: 'Code',
  lastName: 'Btor',
  ssn: '010177-1234'
})

test.use({ evakaOptions: { mockedTime: now } })

let financeAdmin: DevEmployee

test.beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()

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

  await familyWithRestrictedDetailsGuardian.save()

  await Fixture.feeThresholds().save()
})

async function openInvoicesPage(page: Page): Promise<InvoicesPage> {
  financeAdmin = await Fixture.employee().financeAdmin().save()
  await employeeLogin(page, financeAdmin)

  await page.goto(config.employeeUrl)
  const nav = new EmployeeNav(page)
  await nav.openTab('finance')
  const financePage = new FinancePage(page)
  const invoicesPage = await financePage.selectInvoicesTab()

  return invoicesPage
}

test.describe('Invoices', () => {
  test.describe('Create drafts', () => {
    const feeDecision = feeDecisionsFixture(
      'SENT',
      testAdult,
      testChild2,
      testDaycare.id,
      codebtor,
      FiniteDateRange.ofMonth(today.subMonths(1)),
      now,
      fromUuid<FeeDecisionId>('bcc42d48-765d-4fe1-bc90-7a7b4c8205fe'),
      null,
      123123123
    )

    test.beforeEach(async () => {
      await createFeeDecisions({ body: [feeDecision] })
      await Fixture.placement({
        childId: testChild2.id,
        unitId: testDaycare.id,
        startDate: feeDecision.validDuring.start,
        endDate: feeDecision.validDuring.end
      }).save()
    })

    test('List of invoice drafts is empty intially and after creating new drafts the list has one invoice', async ({
      evaka
    }) => {
      const invoicesPage = await openInvoicesPage(evaka)
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(0)
      await invoicesPage.createInvoiceDrafts()
      await invoicesPage.assertInvoiceCount(1)
    })

    test('Invoice page has correct content', async ({ evaka }) => {
      const invoicesPage = await openInvoicesPage(evaka)
      await invoicesPage.searchInvoices()
      await invoicesPage.createInvoiceDrafts()
      const invoicePage = await invoicesPage.openFirstInvoice()

      const head = invoicePage.headOfFamilySection
      await expect(head.headOfFamilyName).toHaveText(
        `${testAdult.firstName} ${testAdult.lastName}`,
        { useInnerText: true }
      )
      await expect(head.headOfFamilySsn).toHaveText(testAdult.ssn!, {
        useInnerText: true
      })
      await expect(head.codebtorName).toHaveText(
        `${codebtor.firstName} ${codebtor.lastName}`,
        { useInnerText: true }
      )
      await expect(head.codebtorSsn).toHaveText(codebtor.ssn!, {
        useInnerText: true
      })

      const details = invoicePage.detailsSection
      await expect(details.status).toHaveText('Luonnos', { useInnerText: true })

      const periodStart = today.subMonths(1).withDate(1)
      const periodEnd = today.withDate(1).subDays(1)
      await expect(details.period).toHaveText(
        `${periodStart.format()} - ${periodEnd.format()}`,
        { useInnerText: true }
      )

      await expect(details.number).toHaveText('', { useInnerText: true })
      await expect(details.dueDate).toHaveText(today.addDays(28).format(), {
        useInnerText: true
      })
      await expect(details.account).toHaveText('3295', { useInnerText: true })
      await expect(details.agreementType).toHaveText('299', {
        useInnerText: true
      })
      await expect(details.relatedFeeDecisions).toHaveText(
        feeDecision.decisionNumber!.toString(),
        { useInnerText: true }
      )
      await expect(details.replacedInvoice).toBeHidden()

      const child = invoicePage.nthChild(0)
      await expect(child.childName).toHaveText(
        `${testChild2.lastName} ${testChild2.firstName}`,
        { useInnerText: true }
      )
      await expect(child.childSsn).toHaveText(testChild2.ssn!, {
        useInnerText: true
      })

      const row = child.row(0)
      await expect(row.product).toHaveText('Varhaiskasvatus', {
        useInnerText: true
      })
      await expect(row.description).toHaveText('', { useInnerText: true })
      await expect(row.unit).toHaveText(testDaycare.name, {
        useInnerText: true
      })
      await expect(row.period).toHaveText(
        `${periodStart.format()} - ${periodEnd.format()}`,
        { useInnerText: true }
      )
      await expect(row.amount).toHaveText('1', { useInnerText: true })
      await expect(row.unitPrice).toHaveText('289', { useInnerText: true })
      await expect(row.totalPrice).toHaveText('289', { useInnerText: true })

      await expect(child.totalPrice).toHaveText('289', { useInnerText: true })
      await expect(child.previousTotalPrice).toBeHidden()

      await expect(invoicePage.totalPrice).toHaveText('289', {
        useInnerText: true
      })
      await expect(invoicePage.previousTotalPrice).toBeHidden()

      await invoicesPage.navigateBackToInvoices()
    })
  })

  test.describe('Send invoices', () => {
    test('Invoices are toggled and sent', async ({ evaka }) => {
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
      const invoicesPage = await openInvoicesPage(evaka)
      await invoicesPage.searchInvoices()
      await invoicesPage.toggleAllInvoices(true)
      await invoicesPage.assertInvoiceCount(2)
      await invoicesPage.sendInvoices()
      await invoicesPage.assertInvoiceCount(0)
      await invoicesPage.filterByStatus('SENT')
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(2)
    })

    test('Filtering invoices result selection resets after change', async ({
      evaka
    }) => {
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

      const invoicesPage = await openInvoicesPage(evaka)
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(2)
      await invoicesPage.selectFirstInvoice()
      await invoicesPage.sendInvoices()
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.filterByStatus('SENT')
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.toggleAllInvoices(true)
      await invoicesPage.filterByStatus('DRAFT')
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.assertButtonsDisabled()
    })

    test('Sending an invoice with a recipient without a SSN', async ({
      evaka
    }) => {
      const adultWithoutSSN = await Fixture.person({
        id: fromUuid<PersonId>('a6cf0ec0-4573-4816-be30-6b87fd943817'),
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

      const invoicesPage = await openInvoicesPage(evaka)
      await invoicesPage.freeTextFilter(adultWithoutSSN.firstName)
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.toggleAllInvoices(true)
      await invoicesPage.sendInvoices()
      await invoicesPage.assertInvoiceCount(0)
      await invoicesPage.filterByStatus('WAITING_FOR_SENDING')
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(1)
      await invoicesPage.openFirstInvoice()
      await invoicesPage.markInvoiceSent()
      await invoicesPage.navigateBackToInvoices()
      await invoicesPage.filterByStatus('SENT')
      await invoicesPage.searchInvoices()
      await invoicesPage.assertInvoiceCount(1)
    })
  })

  test.describe('Replacement invoices', () => {
    const feeDecision = feeDecisionsFixture(
      'SENT',
      testAdult,
      testChild2,
      testDaycare.id,
      codebtor,
      FiniteDateRange.ofMonth(today.subMonths(1)),
      now,
      fromUuid<FeeDecisionId>('bcc42d48-765d-4fe1-bc90-7a7b4c8205fe'),
      null,
      123123123
    )
    let placement: DevPlacement
    let invoicesPage: InvoicesPage

    test.beforeEach(async ({ evaka }) => {
      await createFeeDecisions({ body: [feeDecision] })
      placement = await Fixture.placement({
        childId: testChild2.id,
        unitId: testDaycare.id,
        startDate: feeDecision.validDuring.start,
        endDate: feeDecision.validDuring.end
      }).save()

      invoicesPage = await openInvoicesPage(evaka)
      await invoicesPage.searchInvoices()

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
      await invoicesPage.searchInvoices()

      const invoicePage = await invoicesPage.openFirstInvoice()

      const details = invoicePage.detailsSection
      await expect(details.status).toHaveText('Oikaisuluonnos', {
        useInnerText: true
      })

      await expect(details.relatedFeeDecisions).toHaveText(
        feeDecision.decisionNumber!.toString(),
        { useInnerText: true }
      )
      await expect(details.replacedInvoice).toHaveText('Lasku 10/2024', {
        useInnerText: true
      })

      const child = invoicePage.nthChild(0)
      await expect(child.totalPrice).toHaveText('276,43', {
        useInnerText: true
      })
      await expect(child.previousTotalPrice).toHaveText('289', {
        useInnerText: true
      })

      await expect(invoicePage.totalPrice).toHaveText('276,43', {
        useInnerText: true
      })
      await expect(invoicePage.previousTotalPrice).toHaveText('289', {
        useInnerText: true
      })

      await invoicesPage.navigateBackToInvoices()
    })

    test('Replacement invoice content when there are no rows', async () => {
      // Delete the placement => replacement invoice with no rows is generated
      await deletePlacement({ placementId: placement.id })
      await generateReplacementDraftInvoices()

      await invoicesPage.filterByStatus('REPLACEMENT_DRAFT')
      await invoicesPage.searchInvoices()

      const invoicePage = await invoicesPage.openFirstInvoice()

      const details = invoicePage.detailsSection
      await expect(details.status).toHaveText('Oikaisuluonnos', {
        useInnerText: true
      })

      await expect(details.replacedInvoice).toHaveText('Lasku 10/2024', {
        useInnerText: true
      })

      const child = invoicePage.nthChild(0)
      await expect(child.childName).toHaveText(
        `${testChild2.lastName} ${testChild2.firstName}`,
        { useInnerText: true }
      )
      await expect(child.childSsn).toHaveText(testChild2.ssn!, {
        useInnerText: true
      })
      await expect(child.totalPrice).toHaveText('0', { useInnerText: true })
      await expect(child.previousTotalPrice).toHaveText('289', {
        useInnerText: true
      })

      await expect(invoicePage.totalPrice).toHaveText('0', {
        useInnerText: true
      })
      await expect(invoicePage.previousTotalPrice).toHaveText('289', {
        useInnerText: true
      })

      await invoicesPage.navigateBackToInvoices()
    })

    test('Replacement invoice can be marked as sent', async () => {
      // Add an absence => replacement invoice is generated
      await Fixture.absence({
        childId: testChild2.id,
        date: today.subMonths(1).withDate(1),
        absenceType: 'FORCE_MAJEURE',
        absenceCategory: 'BILLABLE'
      }).save()
      await generateReplacementDraftInvoices()

      await invoicesPage.filterByStatus('REPLACEMENT_DRAFT')
      await invoicesPage.searchInvoices()

      const invoicePage = await invoicesPage.openFirstInvoice()
      const form = invoicePage.replacementDraftForm

      await form.selectReason('ABSENCE')
      await form.notes.fill('Unohtunut päiväkirjamerkintä')
      await form.attachments.upload('src/e2e-test/assets/test_file.jpg')
      await form.markSentButton.click()

      const view = invoicePage.replacementInfo
      await expect(view.reason).toHaveText('Päiväkirjamerkintä', {
        useInnerText: true
      })
      await expect(view.notes).toHaveText('Unohtunut päiväkirjamerkintä', {
        useInnerText: true
      })
      await expect(view.attachments).toHaveCount(1)
      await expect(view.sentAt).toHaveText(now.format(), { useInnerText: true })
      await expect(view.sentBy).toHaveText(
        `${financeAdmin.lastName} ${financeAdmin.firstName}`,
        { useInnerText: true }
      )
    })
  })
})
