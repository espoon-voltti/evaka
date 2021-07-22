// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import LocalDate from 'lib-common/local-date'
import { runPendingAsyncJobs } from 'e2e-test-common/dev-api'
import {
  AsyncButton,
  Checkbox,
  Radio,
  RawElement,
  RawTextInput
} from 'e2e-playwright/utils/element'
import {
  waitUntilEqual,
  waitUntilFalse,
  waitUntilTrue
} from 'e2e-playwright/utils'

export class FinancePage {
  constructor(private readonly page: Page) {}

  async selectFeeDecisionsTab() {
    const elem = new RawElement(this.page, `[data-qa="fee-decisions-tab"]`)
    await elem.click()
    return new FeeDecisionsPage(this.page)
  }

  async selectValueDecisionsTab() {
    const elem = new RawElement(this.page, `[data-qa="value-decisions-tab"]`)
    await elem.click()
    return new ValueDecisionsPage(this.page)
  }

  async selectInvoicesTab() {
    const elem = new RawElement(this.page, `[data-qa="invoices-tab"]`)
    await elem.click()
    const page = new InvoicesPage(this.page)
    await page.invoicesPageIsLoaded()
    return page
  }
}

export class FeeDecisionsPage {
  constructor(private readonly page: Page) {}

  #feeDecisionListPage = new RawElement(
    this.page,
    '[data-qa="fee-decisions-page"]'
  )
  #feeDecisionDetailsPage = new RawElement(
    this.page,
    '[data-qa="fee-decision-details-page"]'
  )
  #feeDecisionRow = new RawElement(
    this.page,
    '[data-qa="table-fee-decision-row"]'
  )
  #navigateBackButton = new RawElement(this.page, '[data-qa="navigate-back"]')
  #statusFilter = {
    sent: new Radio(this.page, '[data-qa="fee-decision-status-filter-SENT"]')
  }
  #allFeeDecisionsToggle = new Checkbox(
    this.page,
    '[data-qa="toggle-all-decisions"]'
  )
  #sendFeeDecisionsButton = new AsyncButton(
    this.page,
    '[data-qa="confirm-decisions"]'
  )

  async getFeeDecisionCount() {
    return this.page.$$eval(
      '[data-qa="table-fee-decision-row"]',
      (rows) => rows.length
    )
  }

  async openFirstFeeDecision() {
    await this.#feeDecisionRow.click()
    await waitUntilTrue(() => this.#feeDecisionDetailsPage.visible)
  }

  async navigateBackFromDetails() {
    await this.#navigateBackButton.click()
    await waitUntilTrue(() => this.#feeDecisionListPage.visible)
  }

  async toggleAllFeeDecisions(toggledOn: boolean) {
    await waitUntilEqual(() => this.#allFeeDecisionsToggle.checked, !toggledOn)
    await this.#allFeeDecisionsToggle.click()
    await waitUntilEqual(() => this.#allFeeDecisionsToggle.checked, toggledOn)
  }

  async sendFeeDecisions() {
    await this.#sendFeeDecisionsButton.click()
    await this.#sendFeeDecisionsButton.waitUntilSuccessful()
    await runPendingAsyncJobs()
  }

  async assertSentDecisionsCount(count: number) {
    await this.#statusFilter.sent.click()
    await waitUntilTrue(() => this.#statusFilter.sent.checked)
    await waitUntilEqual(
      () =>
        this.page.$$eval(
          '[data-qa="table-fee-decision-row"]',
          (rows) => rows.length
        ),
      count
    )
  }
}

export class ValueDecisionsPage {
  constructor(private readonly page: Page) {}

  #valueDecisionListPage = new RawElement(
    this.page,
    '[data-qa="voucher-value-decisions-page"]'
  )
  #valueDecisionDetailsPage = new RawElement(
    this.page,
    '[data-qa="voucher-value-decision-page"]'
  )
  readonly #fromDateInput = new RawTextInput(
    this.page,
    '[data-qa="value-decisions-start-date"] input'
  )
  readonly #toDateInput = new RawTextInput(
    this.page,
    '[data-qa="value-decisions-end-date"] input'
  )
  readonly #dateCheckbox = new RawElement(
    this.page,
    '[data-qa="value-decision-search-by-start-date"]'
  )
  #allValueDecisionsToggle = new Checkbox(
    this.page,
    '[data-qa="toggle-all-decisions"]'
  )
  #sendValueDecisionsButton = new AsyncButton(
    this.page,
    '[data-qa="send-decisions"]'
  )
  #valueDecisionRow = new RawElement(
    this.page,
    '[data-qa="table-value-decision-row"]'
  )
  #navigateBackButton = new RawElement(this.page, '[data-qa="navigate-back"]')
  #statusFilter = {
    sent: new Radio(this.page, '[data-qa="value-decision-status-filter-SENT"]')
  }

  async openFirstValueDecision() {
    await this.#valueDecisionRow.click()
    await waitUntilTrue(() => this.#valueDecisionDetailsPage.visible)
  }

  async navigateBackFromDetails() {
    await this.#navigateBackButton.click()
    await waitUntilTrue(() => this.#valueDecisionListPage.visible)
  }

  async getValueDecisionCount() {
    return this.page.$$eval(
      '[data-qa="table-value-decision-row"]',
      (rows) => rows.length
    )
  }

  async setDates(from: LocalDate, to: LocalDate) {
    await this.#toDateInput.fill(to.format())
    await this.#fromDateInput.fill(from.format())
  }

  async startDateWithinrange() {
    await this.#dateCheckbox.click()
  }

  async toggleAllValueDecisions(toggledOn: boolean) {
    await waitUntilEqual(
      () => this.#allValueDecisionsToggle.checked,
      !toggledOn
    )
    await this.#allValueDecisionsToggle.click()
    await waitUntilEqual(() => this.#allValueDecisionsToggle.checked, toggledOn)
  }

  async sendValueDecisions() {
    await this.#sendValueDecisionsButton.click()
    await this.#sendValueDecisionsButton.waitUntilSuccessful()
    await runPendingAsyncJobs()
  }

  async assertSentDecisionsCount(count: number) {
    await this.#statusFilter.sent.click()
    await waitUntilTrue(() => this.#statusFilter.sent.checked)
    await waitUntilEqual(
      () =>
        this.page.$$eval(
          '[data-qa="table-value-decision-row"]',
          (rows) => rows.length
        ),
      count
    )
  }
}

export class InvoicesPage {
  constructor(private readonly page: Page) {}

  #invoicesPage = new RawElement(this.page, '[data-qa="invoices-page"]')
  #invoiceDetailsPage = new RawElement(
    this.page,
    '[data-qa="invoice-details-page"]'
  )
  #spinner = new RawElement(this.page, '.loader-spinner')
  #createInvoicesButton = new RawElement(
    this.page,
    '[data-qa="create-invoices"]'
  )
  #invoiceInList = new RawElement(this.page, '[data-qa="table-invoice-row"]')
  #allInvoicesToggle = new Checkbox(
    this.page,
    '[data-qa="toggle-all-invoices"]'
  )
  #openSendInvoicesDialogButton = new RawElement(
    this.page,
    '[data-qa="open-send-invoices-dialog"]'
  )
  #sendInvoicesDialog = new RawElement(
    this.page,
    '[data-qa="send-invoices-dialog"]'
  )
  #sendInvoicesButton = new AsyncButton(
    this.page,
    '[data-qa="send-invoices-dialog"] [data-qa="modal-okBtn"]'
  )
  #navigateBack = new RawElement(this.page, '[data-qa="navigate-back"]')
  #invoiceDetailsHeadOfFamily = new RawElement(
    this.page,
    '[data-qa="invoice-details-head-of-family"]'
  )
  #addInvoiceRowButton = new RawElement(
    this.page,
    '[data-qa="invoice-button-add-row"]'
  )
  #invoiceRow = (index: number) => {
    const row = new RawElement(
      this.page,
      `[data-qa="invoice-details-invoice-row"]:nth-child(${index + 1})`
    )
    return {
      costCenterInput: row.findInput('[data-qa="input-cost-center"]'),
      amountInput: row.findInput('[data-qa="input-amount"]'),
      unitPriceInput: row.findInput('[data-qa="input-price"]'),
      deleteRowButton: row.find('[data-qa="delete-invoice-row-button"]')
    }
  }
  #saveChangesButton = new AsyncButton(
    this.page,
    '[data-qa="invoice-actions-save-changes"]'
  )
  #markInvoiceSentButton = new AsyncButton(
    this.page,
    '[data-qa="invoice-actions-mark-sent"]'
  )

  async invoicesPageIsLoaded() {
    await this.#invoicesPage.waitUntilVisible()
    await waitUntilFalse(() => this.#spinner.visible)
  }

  async createInvoiceDrafts() {
    await this.#createInvoicesButton.click()
    await waitUntilFalse(() => this.#spinner.visible)
  }

  async assertInvoiceCount(count: number) {
    await waitUntilEqual(
      () =>
        this.page.$$eval(
          '[data-qa="table-invoice-row"]',
          (rows) => rows.length
        ),
      count
    )
  }

  async toggleAllInvoices(toggled: boolean) {
    await waitUntilEqual(() => this.#allInvoicesToggle.checked, !toggled)
    await this.#allInvoicesToggle.click()
    await waitUntilEqual(() => this.#allInvoicesToggle.checked, toggled)
  }

  async sendInvoices() {
    await this.#openSendInvoicesDialogButton.click()
    await this.#sendInvoicesDialog.waitUntilVisible()
    await this.#sendInvoicesButton.click()
    await this.#sendInvoicesButton.waitUntilSuccessful()
  }

  async showSentInvoices() {
    await new Radio(this.page, '[data-qa="invoice-status-filter-SENT"]').click()
  }

  async showWaitingForSendingInvoices() {
    await new Radio(
      this.page,
      '[data-qa="invoice-status-filter-WAITING_FOR_SENDING"]'
    ).click()
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
        this.page.$$eval(
          '[data-qa="invoice-details-invoice-row"]',
          (rows) => rows.length
        ),
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
    await invoiceRow.costCenterInput.clear()
    await invoiceRow.costCenterInput.type(costCenter)
    await invoiceRow.amountInput.clear()
    await invoiceRow.amountInput.type(this.formatFinnishDecimal(amount))
    await invoiceRow.unitPriceInput.clear()
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
    await new RawTextInput(
      this.page,
      '[data-qa="free-text-search-input"]'
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
