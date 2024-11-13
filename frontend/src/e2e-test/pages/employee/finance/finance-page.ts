// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ProviderType } from 'lib-common/generated/api-types/daycare'
import {
  FeeDecisionStatus,
  PaymentStatus,
  VoucherValueDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import { runPendingAsyncJobs } from '../../../dev-api'
import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import {
  AsyncButton,
  Checkable,
  Checkbox,
  DatePickerDeprecated,
  Page,
  Radio,
  Select,
  TextInput,
  Element,
  ElementCollection
} from '../../../utils/page'
import ChildInformationPage from '../child-information'
import GuardianInformationPage from '../guardian-information'

import { InvoiceDetailsPage } from './invoice-details-page'

export class FinancePage {
  constructor(private readonly page: Page) {}

  async selectFeeDecisionsTab() {
    await this.page.findByDataQa(`fee-decisions-tab`).click()
    return new FeeDecisionsPage(this.page)
  }

  async selectValueDecisionsTab() {
    await this.page.findByDataQa(`value-decisions-tab`).click()
    return new ValueDecisionsPage(this.page)
  }

  async selectInvoicesTab() {
    await this.page.findByDataQa(`invoices-tab`).click()
    const page = new InvoicesPage(this.page)
    await page.assertLoaded()
    return page
  }

  async selectIncomeStatementsTab() {
    await this.page.findByDataQa(`income-statements-tab`).click()
  }

  async selectPaymentsTab() {
    await this.page.findByDataQa(`payments-tab`).click()
    return new PaymentsPage(this.page)
  }
}

export class FeeDecisionsPage {
  #feeDecisionListPage: Element
  #navigateBackButton: Element
  #allFeeDecisionsToggle: Checkbox
  #sendFeeDecisionsButton: AsyncButton
  #openDecisionHandlerSelectModalButton: AsyncButton
  #firstFeeDecisionRow: Element

  constructor(private readonly page: Page) {
    this.#feeDecisionListPage = page.findByDataQa('fee-decisions-page')
    this.#navigateBackButton = page.findByDataQa('navigate-back')
    this.#allFeeDecisionsToggle = new Checkbox(
      page.findByDataQa('toggle-all-decisions')
    )
    this.#sendFeeDecisionsButton = new AsyncButton(
      page.findByDataQa('confirm-decisions')
    )
    this.#openDecisionHandlerSelectModalButton = new AsyncButton(
      page.findByDataQa('open-decision-handler-select-modal')
    )
    this.#firstFeeDecisionRow = page
      .findAll('[data-qa="table-fee-decision-row"]')
      .first()
  }

  #statusFilter = (status: FeeDecisionStatus) =>
    new Checkbox(this.page.findByDataQa(`fee-decision-status-filter-${status}`))

  async getFeeDecisionCount() {
    return this.page.findAll('[data-qa="table-fee-decision-row"]').count()
  }

  async openFirstFeeDecision(): Promise<FeeDecisionDetailsPage> {
    const popup = await this.page.capturePopup(async () => {
      await this.#firstFeeDecisionRow.click()
    })
    const detailsPage = new FeeDecisionDetailsPage(popup)
    await detailsPage.waitUntilVisible()
    return detailsPage
  }

  async navigateBackFromDetails() {
    await this.#navigateBackButton.click()
    await this.#feeDecisionListPage.waitUntilVisible()
  }

  async toggleAllFeeDecisions(toggledOn: boolean) {
    await this.#allFeeDecisionsToggle.waitUntilChecked(!toggledOn)
    await this.#allFeeDecisionsToggle.click()
    await this.#allFeeDecisionsToggle.waitUntilChecked(toggledOn)
  }

  async sendFeeDecisions(mockedNow: HelsinkiDateTime) {
    await this.#sendFeeDecisionsButton.click()
    await this.#sendFeeDecisionsButton.waitUntilIdle()
    await runPendingAsyncJobs(mockedNow)
  }

  async openDecisionHandlerModal() {
    await this.#openDecisionHandlerSelectModalButton.click()
    return new FinanceDecisionHandlerSelectModal(this.page)
  }

  async assertSentDecisionsCount(count: number) {
    await this.#statusFilter('DRAFT').click()
    await this.#statusFilter('SENT').click()
    await this.#statusFilter('SENT').waitUntilChecked()
    await waitUntilEqual(
      () => this.page.findAll('[data-qa="table-fee-decision-row"]').count(),
      count
    )
  }
}

export class FeeDecisionDetailsPage {
  #partnerName: Element
  #headOfFamily: Element
  #decisionHandler: Element
  #openDecisionHandlerSelectModalButton: AsyncButton
  #childIncome: ElementCollection

  constructor(private readonly page: Page) {
    this.#partnerName = page.findByDataQa('partner')
    this.#headOfFamily = page.findByDataQa('head-of-family')
    this.#decisionHandler = page.findByDataQa('decision-handler')
    this.#openDecisionHandlerSelectModalButton = new AsyncButton(
      page.findByDataQa('open-decision-handler-select-modal')
    )
    this.#childIncome = page.findAll('[data-qa="child-income"]')
  }

  async assertPartnerName(expectedName: string) {
    await this.#partnerName.assertTextEquals(expectedName)
  }

  async assertDecisionHandler(expectedName: string) {
    await this.#decisionHandler.assertTextEquals(expectedName)
  }

  async assertChildIncome(nth: number, expectedTotalText: string) {
    await waitUntilTrue(async () =>
      (await this.#childIncome.nth(nth).text).includes(expectedTotalText)
    )
  }

  async assertPartnerNameNotShown() {
    await this.#headOfFamily.waitUntilVisible()
    await this.#partnerName.waitUntilHidden()
  }

  async waitUntilVisible() {
    await this.page
      .find('[data-qa="fee-decision-details-page"]')
      .waitUntilVisible()
  }

  async openDecisionHandlerModal() {
    await this.#openDecisionHandlerSelectModalButton.click()
    return new FinanceDecisionHandlerSelectModal(this.page)
  }
}

export class ValueDecisionsPage {
  #fromDateInput: DatePickerDeprecated
  #toDateInput: DatePickerDeprecated
  #dateCheckbox: Checkbox
  #allValueDecisionsToggle: Checkbox
  #sendValueDecisionsButton: AsyncButton
  #openDecisionHandlerSelectModalButton: AsyncButton
  #firstValueDecisionRow: Element

  constructor(private readonly page: Page) {
    this.#fromDateInput = new DatePickerDeprecated(
      page.findByDataQa('value-decisions-start-date')
    )
    this.#toDateInput = new DatePickerDeprecated(
      page.findByDataQa('value-decisions-end-date')
    )
    this.#dateCheckbox = new Checkbox(
      page.findByDataQa('value-decision-search-by-start-date')
    )
    this.#allValueDecisionsToggle = new Checkbox(
      page.findByDataQa('toggle-all-decisions')
    )
    this.#sendValueDecisionsButton = new AsyncButton(
      page.findByDataQa('send-decisions')
    )
    this.#openDecisionHandlerSelectModalButton = new AsyncButton(
      page.findByDataQa('open-decision-handler-select-modal')
    )
    this.#firstValueDecisionRow = page
      .findAll('[data-qa="table-value-decision-row"]')
      .first()
  }

  #statusFilter = (status: VoucherValueDecisionStatus) =>
    new Checkbox(
      this.page.findByDataQa(`value-decision-status-filter-${status}`)
    )

  async openFirstValueDecision(): Promise<ValueDecisionDetailsPage> {
    const popup = await this.page.capturePopup(async () => {
      await this.#firstValueDecisionRow.click()
    })
    const detailsPage = new ValueDecisionDetailsPage(popup)
    await detailsPage.waitUntilVisible()
    return detailsPage
  }

  async getValueDecisionCount() {
    return this.page.findAll('[data-qa="table-value-decision-row"]').count()
  }

  async setDates(from: LocalDate, to: LocalDate) {
    await this.#toDateInput.fill(to.format())
    await this.#fromDateInput.fill(from.format())
  }

  async startDateWithinRange() {
    await this.#dateCheckbox.check()
  }

  async toggleAllValueDecisions() {
    await this.#allValueDecisionsToggle.find('input').click()
  }

  async sendValueDecisions(mockedNow: HelsinkiDateTime) {
    await this.#sendValueDecisionsButton.click()
    await this.#sendValueDecisionsButton.waitUntilIdle()
    await runPendingAsyncJobs(mockedNow)
  }

  async openDecisionHandlerModal() {
    await this.#openDecisionHandlerSelectModalButton.click()
    return new FinanceDecisionHandlerSelectModal(this.page)
  }

  async assertSentDecisionsCount(count: number) {
    await this.#statusFilter('DRAFT').click()
    await this.#statusFilter('SENT').click()
    await this.#statusFilter('SENT').waitUntilChecked()
    await waitUntilEqual(
      () => this.page.findAll('[data-qa="table-value-decision-row"]').count(),
      count
    )
  }
}

export class ValueDecisionDetailsPage {
  #partnerName: Element
  #headOfFamily: Element
  #decisionHandler: Element
  #sendDecisionButton: Element
  #openDecisionHandlerSelectModalButton: AsyncButton
  #childIncome: ElementCollection

  constructor(private readonly page: Page) {
    this.#partnerName = page.findByDataQa('partner')
    this.#headOfFamily = page.findByDataQa('head-of-family')
    this.#decisionHandler = page.findByDataQa('decision-handler')
    this.#sendDecisionButton = page.findByDataQa('button-send-decision')
    this.#openDecisionHandlerSelectModalButton = new AsyncButton(
      page.findByDataQa('open-decision-handler-select-modal')
    )
    this.#childIncome = page.findAll('[data-qa="child-income"]')
  }

  async sendValueDecision() {
    await this.#sendDecisionButton.click()
    await this.#sendDecisionButton.waitUntilHidden()
  }

  async assertPartnerName(expectedName: string) {
    await this.#partnerName.assertTextEquals(expectedName)
  }

  async assertPartnerNameNotShown() {
    await this.#headOfFamily.waitUntilVisible()
    await this.#partnerName.waitUntilHidden()
  }

  async assertDecisionHandler(expectedName: string) {
    await this.#decisionHandler.assertTextEquals(expectedName)
  }

  async assertChildIncome(nth: number, expectedTotalText: string) {
    await waitUntilTrue(async () =>
      (await this.#childIncome.nth(nth).text).includes(expectedTotalText)
    )
  }

  async waitUntilVisible() {
    await this.page
      .find('[data-qa="voucher-value-decision-page"]')
      .waitUntilVisible()
  }

  async openDecisionHandlerModal() {
    await this.#openDecisionHandlerSelectModalButton.click()
    return new FinanceDecisionHandlerSelectModal(this.page)
  }
}

export class FinanceDecisionHandlerSelectModal {
  #decisionHandlerSelect: Select
  #decisionHandlerSelectModalResolveBtn: AsyncButton
  #decisionHandlerSelectModalRejectBtn: AsyncButton

  constructor(readonly page: Page) {
    this.#decisionHandlerSelect = new Select(
      page.findByDataQa('finance-decision-handler-select')
    )
    this.#decisionHandlerSelectModalResolveBtn = new AsyncButton(
      page.findByDataQa('modal-okBtn')
    )
    this.#decisionHandlerSelectModalRejectBtn = new AsyncButton(
      page.findByDataQa('modal-cancelBtn')
    )
  }

  async selectDecisionHandler(value: string) {
    await this.#decisionHandlerSelect.selectOption({ value })
  }

  async resolveDecisionHandlerModal(mockedNow: HelsinkiDateTime) {
    await this.#decisionHandlerSelectModalResolveBtn.click()
    await this.#decisionHandlerSelectModalResolveBtn.waitUntilHidden()
    await runPendingAsyncJobs(mockedNow)
  }

  async rejectDecisionHandlerModal(mockedNow: HelsinkiDateTime) {
    await this.#decisionHandlerSelectModalRejectBtn.click()
    await this.#decisionHandlerSelectModalRejectBtn.waitUntilHidden()
    await runPendingAsyncJobs(mockedNow)
  }
}

export class InvoicesPage {
  #invoicesPage: Element
  #invoiceDetailsPage: Element
  #createInvoicesButton: Element
  #invoiceInList: Element
  #allInvoicesToggle: Checkbox
  #openSendInvoicesDialogButton: Element
  #sendInvoicesDialog: Element
  #navigateBack: Element
  #invoiceDetailsHeadOfFamily: Element
  #markInvoiceSentButton: AsyncButton
  #invoices: Element
  #sendInvoicesButton: AsyncButton

  constructor(private readonly page: Page) {
    this.#invoicesPage = page.findByDataQa('invoices-page')
    this.#invoiceDetailsPage = page.findByDataQa('invoice-details-page')
    this.#createInvoicesButton = page.findByDataQa('create-invoices')
    this.#invoiceInList = page.findByDataQa('table-invoice-row')
    this.#allInvoicesToggle = new Checkbox(
      page.findByDataQa('toggle-all-invoices')
    )
    this.#openSendInvoicesDialogButton = page.findByDataQa(
      'open-send-invoices-dialog'
    )
    this.#sendInvoicesDialog = page.findByDataQa('send-invoices-dialog')
    this.#navigateBack = page.findByDataQa('navigate-back')
    this.#invoiceDetailsHeadOfFamily = page.findByDataQa(
      'invoice-details-head-of-family'
    )
    this.#markInvoiceSentButton = new AsyncButton(
      page.findByDataQa('invoice-actions-mark-sent')
    )
    this.#invoices = page.find('.invoices')
    this.#sendInvoicesButton = new AsyncButton(
      page.find('[data-qa="send-invoices-dialog"] [data-qa="modal-okBtn"]')
    )
  }

  async assertLoaded() {
    await this.#invoicesPage.waitUntilVisible()
    await this.#invoices.assertAttributeEquals('data-isloading', 'false')
  }

  async createInvoiceDrafts() {
    await this.#createInvoicesButton.click()
    await this.assertLoaded()
  }

  async assertInvoiceCount(count: number) {
    await waitUntilEqual(
      () => this.page.findAll('[data-qa="table-invoice-row"]').count(),
      count
    )
  }

  async toggleAllInvoices(toggled: boolean) {
    await this.#allInvoicesToggle.waitUntilChecked(!toggled)
    await this.#allInvoicesToggle.click()
    await this.#allInvoicesToggle.waitUntilChecked(toggled)
  }

  async sendInvoices() {
    await this.#openSendInvoicesDialogButton.click()
    await this.#sendInvoicesDialog.waitUntilVisible()
    await this.#sendInvoicesDialog.find('[data-qa="title"]').click()
    await this.#sendInvoicesButton.click()
    await this.#sendInvoicesButton.waitUntilHidden()
  }

  async showSentInvoices() {
    await this.page.findByDataQa('invoice-status-filter-SENT').click()
  }

  async showWaitingForSendingInvoices() {
    await this.page
      .find('[data-qa="invoice-status-filter-WAITING_FOR_SENDING"]')
      .click()
  }

  async openFirstInvoice(): Promise<InvoiceDetailsPage> {
    await this.#invoiceInList.click()
    await this.#invoiceDetailsPage.waitUntilVisible()
    return new InvoiceDetailsPage(this.page)
  }

  async assertInvoiceHeadOfFamily(fullName: string) {
    await this.#invoiceDetailsPage.waitUntilVisible()
    await this.#invoiceDetailsHeadOfFamily.assertTextEquals(fullName)
  }

  async navigateBackToInvoices() {
    await this.#navigateBack.click()
    await this.#invoicesPage.waitUntilVisible()
  }

  async assertInvoiceRowCount(count: number) {
    await waitUntilEqual(
      () =>
        this.page.findAll('[data-qa="invoice-details-invoice-row"]').count(),
      count
    )
  }

  async assertInvoiceTotal(total: number) {
    await this.#invoiceInList
      .find('[data-qa="invoice-total"]')
      .assertTextEquals(this.formatFinnishDecimal(total))
  }

  async freeTextFilter(text: string) {
    await new TextInput(this.page.findByDataQa('free-text-search-input')).type(
      text
    )
  }

  async markInvoiceSent() {
    await this.#markInvoiceSentButton.click()
    await this.#markInvoiceSentButton.waitUntilHidden()
  }

  private formatFinnishDecimal(number: number) {
    return String(number).replace('.', ',')
  }
}

export class IncomeStatementsPage {
  searchButton: Element
  incomeStatementRows: ElementCollection

  constructor(private readonly page: Page) {
    this.incomeStatementRows = page.findAll(`[data-qa="income-statement-row"]`)
    this.searchButton = page.findByDataQa('search-button')
  }

  #providerTypeFilter = (type: ProviderType) =>
    new Checkable(this.page.findByDataQa(`provider-type-filter-${type}`))

  async waitUntilLoaded() {
    await this.page
      .findByDataQa('income-statements-page')
      .assertAttributeEquals('data-isloading', 'false')
  }

  async selectProviderType(type: ProviderType) {
    await this.#providerTypeFilter(type).check()
  }

  async unSelectProviderType(type: ProviderType) {
    await this.#providerTypeFilter(type).uncheck()
  }

  async openNthIncomeStatementForGuardian(nth: number) {
    await this.incomeStatementRows.nth(nth).findByDataQa('person-link').click()
    const page = new GuardianInformationPage(this.page)
    await page.waitUntilLoaded()
    return page
  }

  async openNthIncomeStatementForChild(nth: number) {
    await this.incomeStatementRows.nth(nth).findByDataQa('person-link').click()
    const page = new ChildInformationPage(this.page)
    await page.waitUntilLoaded()
    return page
  }

  async assertNthIncomeStatement(
    nth: number,
    expectedName: string,
    expectedTypeText: string
  ) {
    await this.incomeStatementRows
      .nth(nth)
      .findByDataQa('person-link')
      .assertTextEquals(expectedName)
    await this.incomeStatementRows
      .nth(nth)
      .findByDataQa('income-statement-type')
      .assertTextEquals(expectedTypeText)
  }
}

export class PaymentsPage {
  constructor(private readonly page: Page) {}

  async setStatusFilter(status: PaymentStatus) {
    const radio = new Radio(this.page.findByDataQa(`status-filter-${status}`))
    if (await radio.checked) {
      return
    }
    await radio.click()
  }

  async assertPaymentCount(count: number) {
    await waitUntilEqual(
      () => this.page.findAllByDataQa('table-payment-row').count(),
      count
    )
  }

  async togglePayments(toggled: boolean) {
    const toggle = new Checkbox(this.page.findByDataQa('toggle-all-payments'))
    await toggle.waitUntilChecked(!toggled)
    await toggle.click()
    await toggle.waitUntilChecked(toggled)
  }

  async sendPayments() {
    await this.page.findByDataQa('open-send-payments-dialog').click()
    const modal = this.page.findByDataQa('send-payments-modal')
    await modal.waitUntilVisible()
    const sendButton = new AsyncButton(modal.findByDataQa('modal-okBtn'))
    await sendButton.click()
    await modal.waitUntilHidden()
  }

  async deletePayments() {
    await this.page.findByDataQa('delete-payments').click()
  }

  async confirmPayments() {
    await this.page.findByDataQa('confirm-payments').click()
  }

  async revertPayments() {
    await this.page.findByDataQa('revert-payments').click()
  }
}
