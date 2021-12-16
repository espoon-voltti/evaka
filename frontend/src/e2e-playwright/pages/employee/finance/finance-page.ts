// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { runPendingAsyncJobs } from 'e2e-test-common/dev-api'
import {
  AsyncButton,
  Checkbox,
  DatePickerDeprecated,
  Page,
  TextInput
} from 'e2e-playwright/utils/page'
import { waitUntilEqual } from 'e2e-playwright/utils'

export class FinancePage {
  constructor(private readonly page: Page) {}

  async selectFeeDecisionsTab() {
    await this.page.find(`[data-qa="fee-decisions-tab"]`).click()
    return new FeeDecisionsPage(this.page)
  }

  async selectValueDecisionsTab() {
    await this.page.find(`[data-qa="value-decisions-tab"]`).click()
    return new ValueDecisionsPage(this.page)
  }

  async selectInvoicesTab() {
    await this.page.find(`[data-qa="invoices-tab"]`).click()
    const page = new InvoicesPage(this.page)
    await page.invoicesPageIsLoaded()
    return page
  }

  async selectIncomeStatementsTab() {
    await this.page.find(`[data-qa="income-statements-tab"]`).click()
  }
}

export class FeeDecisionsPage {
  constructor(private readonly page: Page) {}

  #feeDecisionListPage = this.page.find('[data-qa="fee-decisions-page"]')
  #feeDecisionDetailsPage = this.page.find(
    '[data-qa="fee-decision-details-page"]'
  )
  #firstFeeDecisionRow = this.page
    .findAll('[data-qa="table-fee-decision-row"]')
    .first()
  #navigateBackButton = this.page.find('[data-qa="navigate-back"]')
  #statusFilter = {
    sent: new Checkbox(
      this.page.find('[data-qa="fee-decision-status-filter-SENT"]')
    )
  }
  #allFeeDecisionsToggle = new Checkbox(
    this.page.find('[data-qa="toggle-all-decisions"]')
  )
  #sendFeeDecisionsButton = new AsyncButton(
    this.page.find('[data-qa="confirm-decisions"]')
  )

  async getFeeDecisionCount() {
    return this.page.findAll('[data-qa="table-fee-decision-row"]').count()
  }

  async openFirstFeeDecision() {
    await this.#firstFeeDecisionRow.click()
    await this.#feeDecisionDetailsPage.waitUntilVisible()
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

  async sendFeeDecisions() {
    await this.#sendFeeDecisionsButton.click()
    await this.#sendFeeDecisionsButton.waitUntilSuccessful()
    await runPendingAsyncJobs()
  }

  async assertSentDecisionsCount(count: number) {
    await this.#statusFilter.sent.check()
    await waitUntilEqual(
      () => this.page.findAll('[data-qa="table-fee-decision-row"]').count(),
      count
    )
  }
}

export class FeeDecisionDetailsPage {
  constructor(private readonly page: Page) {}

  #partnerName = this.page.find('[data-qa="partner"]')
  #headOfFamily = this.page.find('[data-qa="head-of-family"]')

  async assertPartnerName(expectedName: string) {
    await waitUntilEqual(() => this.#partnerName.innerText, expectedName)
  }

  async assertPartnerNameNotShown() {
    await this.#headOfFamily.waitUntilVisible()
    await this.#partnerName.waitUntilHidden()
  }
}

export class ValueDecisionsPage {
  constructor(private readonly page: Page) {}

  #valueDecisionListPage = this.page.find(
    '[data-qa="voucher-value-decisions-page"]'
  )
  #valueDecisionDetailsPage = this.page.find(
    '[data-qa="voucher-value-decision-page"]'
  )
  readonly #fromDateInput = new DatePickerDeprecated(
    this.page.find('[data-qa="value-decisions-start-date"]')
  )
  readonly #toDateInput = new DatePickerDeprecated(
    this.page.find('[data-qa="value-decisions-end-date"]')
  )
  readonly #dateCheckbox = new Checkbox(
    this.page.find('[data-qa="value-decision-search-by-start-date"]')
  )
  #allValueDecisionsToggle = new Checkbox(
    this.page.find('[data-qa="toggle-all-decisions"]')
  )
  #sendValueDecisionsButton = new AsyncButton(
    this.page.find('[data-qa="send-decisions"]')
  )
  #firstValueDecisionRow = this.page
    .findAll('[data-qa="table-value-decision-row"]')
    .first()
  #navigateBackButton = this.page.find('[data-qa="navigate-back"]')
  #statusFilter = {
    sent: new Checkbox(
      this.page.find('[data-qa="value-decision-status-filter-SENT"]')
    )
  }

  async openFirstValueDecision() {
    await this.#firstValueDecisionRow.click()
    await this.#valueDecisionDetailsPage.waitUntilVisible()
  }

  async navigateBackFromDetails() {
    await this.#navigateBackButton.click()
    await this.#valueDecisionListPage.waitUntilVisible()
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
    await this.#allValueDecisionsToggle.check()
  }

  async sendValueDecisions() {
    await this.#sendValueDecisionsButton.click()
    await this.#sendValueDecisionsButton.waitUntilSuccessful()
    await runPendingAsyncJobs()
  }

  async assertSentDecisionsCount(count: number) {
    await this.#statusFilter.sent.click()
    await this.#statusFilter.sent.waitUntilChecked()
    await waitUntilEqual(
      () => this.page.findAll('[data-qa="table-value-decision-row"]').count(),
      count
    )
  }
}

export class ValueDecisionDetailsPage {
  constructor(private readonly page: Page) {}

  #partnerName = this.page.find('[data-qa="partner"]')
  #headOfFamily = this.page.find('[data-qa="head-of-family"]')

  #sendDecisionButton = this.page.find('[data-qa="button-send-decision"]')

  async sendValueDecision() {
    await this.#sendDecisionButton.click()
    await this.#sendDecisionButton.waitUntilHidden()
  }

  async assertPartnerName(expectedName: string) {
    await waitUntilEqual(() => this.#partnerName.innerText, expectedName)
  }

  async assertPartnerNameNotShown() {
    await this.#headOfFamily.waitUntilVisible()
    await this.#partnerName.waitUntilHidden()
  }
}

export class InvoicesPage {
  constructor(private readonly page: Page) {}

  #invoicesPage = this.page.find('[data-qa="invoices-page"]')
  #invoiceDetailsPage = this.page.find('[data-qa="invoice-details-page"]')
  #spinner = this.page.find('.loader-spinner')
  #createInvoicesButton = this.page.find('[data-qa="create-invoices"]')
  #invoiceInList = this.page.find('[data-qa="table-invoice-row"]')
  #allInvoicesToggle = new Checkbox(
    this.page.find('[data-qa="toggle-all-invoices"]')
  )
  #openSendInvoicesDialogButton = this.page.find(
    '[data-qa="open-send-invoices-dialog"]'
  )
  #sendInvoicesDialog = this.page.find('[data-qa="send-invoices-dialog"]')
  #sendInvoicesButton = new AsyncButton(
    this.page.find('[data-qa="send-invoices-dialog"] [data-qa="modal-okBtn"]')
  )
  #navigateBack = this.page.find('[data-qa="navigate-back"]')
  #invoiceDetailsHeadOfFamily = this.page.find(
    '[data-qa="invoice-details-head-of-family"]'
  )
  #addInvoiceRowButton = this.page.find('[data-qa="invoice-button-add-row"]')
  #invoiceRow = (index: number) => {
    const row = this.page.find(
      `[data-qa="invoice-details-invoice-row"]:nth-child(${index + 1})`
    )
    return {
      costCenterInput: new TextInput(row.find('[data-qa="input-cost-center"]')),
      amountInput: new TextInput(row.find('[data-qa="input-amount"]')),
      unitPriceInput: new TextInput(row.find('[data-qa="input-price"]')),
      deleteRowButton: new TextInput(
        row.find('[data-qa="delete-invoice-row-button"]')
      )
    }
  }
  #saveChangesButton = new AsyncButton(
    this.page.find('[data-qa="invoice-actions-save-changes"]')
  )
  #markInvoiceSentButton = new AsyncButton(
    this.page.find('[data-qa="invoice-actions-mark-sent"]')
  )

  async invoicesPageIsLoaded() {
    await this.#invoicesPage.waitUntilVisible()
    await this.#spinner.waitUntilHidden()
  }

  async createInvoiceDrafts() {
    await this.#createInvoicesButton.click()
    await this.#spinner.waitUntilHidden()
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
    await this.#sendInvoicesButton.click()
    await this.#sendInvoicesButton.waitUntilSuccessful()
  }

  async showSentInvoices() {
    await this.page.find('[data-qa="invoice-status-filter-SENT"]').click()
  }

  async showWaitingForSendingInvoices() {
    await this.page
      .find('[data-qa="invoice-status-filter-WAITING_FOR_SENDING"]')
      .click()
  }

  async openFirstInvoice() {
    await this.#invoiceInList.click()
    await this.#invoiceDetailsPage.waitUntilVisible()
  }

  async assertInvoiceHeadOfFamily(fullName: string) {
    await this.#invoiceDetailsPage.waitUntilVisible()
    await waitUntilEqual(
      () => this.#invoiceDetailsHeadOfFamily.innerText,
      fullName
    )
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

  async addNewInvoiceRow(
    costCenter: string,
    amount: number,
    unitPrice: number
  ) {
    await this.#addInvoiceRowButton.click()
    const invoiceRow = this.#invoiceRow(1)
    await invoiceRow.costCenterInput.fill('')
    await invoiceRow.costCenterInput.type(costCenter)
    await invoiceRow.amountInput.fill('')
    await invoiceRow.amountInput.type(this.formatFinnishDecimal(amount))
    await invoiceRow.unitPriceInput.fill('')
    await invoiceRow.unitPriceInput.type(this.formatFinnishDecimal(unitPrice))
    await this.#saveChangesButton.click()
    await this.#saveChangesButton.waitUntilSuccessful()
  }

  async deleteInvoiceRow(index: number) {
    await this.#invoiceRow(index).deleteRowButton.click()
    await this.#saveChangesButton.click()
    await this.#saveChangesButton.waitUntilSuccessful()
  }

  async assertInvoiceTotal(total: number) {
    await waitUntilEqual(
      () => this.#invoiceInList.find('[data-qa="invoice-total"]').innerText,
      this.formatFinnishDecimal(total)
    )
  }

  async freeTextFilter(text: string) {
    await new TextInput(
      this.page.find('[data-qa="free-text-search-input"]')
    ).type(text)
  }

  async markInvoiceSent() {
    await this.#markInvoiceSentButton.click()
    await this.#markInvoiceSentButton.waitUntilSuccessful()
  }

  private formatFinnishDecimal(number: number) {
    return String(number).replace('.', ',')
  }
}

export class IncomeStatementsPage {
  constructor(private readonly page: Page) {}

  #incomeStatementRows = this.page.findAll(`[data-qa="income-statement-row"]`)

  async getRowCount(): Promise<number> {
    return this.#incomeStatementRows.count()
  }

  async openNthIncomeStatement(nth: number) {
    await this.#incomeStatementRows.nth(nth).find('a').click()
  }
}
