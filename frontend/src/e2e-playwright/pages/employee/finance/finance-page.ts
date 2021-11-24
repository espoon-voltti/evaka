// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import LocalDate from 'lib-common/local-date'
import { runPendingAsyncJobs } from 'e2e-test-common/dev-api'
import { AsyncButton, CheckboxLocator } from 'e2e-playwright/utils/element'
import { waitUntilEqual } from 'e2e-playwright/utils'

export class FinancePage {
  constructor(private readonly page: Page) {}

  async selectFeeDecisionsTab() {
    await this.page.locator(`[data-qa="fee-decisions-tab"]`).click()
    return new FeeDecisionsPage(this.page)
  }

  async selectValueDecisionsTab() {
    await this.page.locator(`[data-qa="value-decisions-tab"]`).click()
    return new ValueDecisionsPage(this.page)
  }

  async selectInvoicesTab() {
    await this.page.locator(`[data-qa="invoices-tab"]`).click()
    const page = new InvoicesPage(this.page)
    await page.invoicesPageIsLoaded()
    return page
  }

  async selectIncomeStatementsTab() {
    await this.page.locator(`[data-qa="income-statements-tab"]`).click()
  }
}

export class FeeDecisionsPage {
  constructor(private readonly page: Page) {}

  #feeDecisionListPage = this.page.locator('[data-qa="fee-decisions-page"]')
  #feeDecisionDetailsPage = this.page.locator(
    '[data-qa="fee-decision-details-page"]'
  )
  #firstFeeDecisionRow = this.page
    .locator('[data-qa="table-fee-decision-row"]')
    .first()
  #navigateBackButton = this.page.locator('[data-qa="navigate-back"]')
  #statusFilter = {
    sent: new CheckboxLocator(
      this.page.locator('[data-qa="fee-decision-status-filter-SENT"]')
    )
  }
  #allFeeDecisionsToggle = new CheckboxLocator(
    this.page.locator('[data-qa="toggle-all-decisions"]')
  )
  #sendFeeDecisionsButton = new AsyncButton(
    this.page.locator('[data-qa="confirm-decisions"]')
  )

  async getFeeDecisionCount() {
    return this.page.$$eval(
      '[data-qa="table-fee-decision-row"]',
      (rows) => rows.length
    )
  }

  async openFirstFeeDecision() {
    await this.#firstFeeDecisionRow.click()
    await this.#feeDecisionDetailsPage.waitFor()
  }

  async navigateBackFromDetails() {
    await this.#navigateBackButton.click()
    await this.#feeDecisionListPage.waitFor()
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
    await this.#statusFilter.sent.click()
    await this.#statusFilter.sent.waitUntilChecked()
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

export class FeeDecisionDetailsPage {
  constructor(private readonly page: Page) {}

  #partnerName = this.page.locator('[data-qa="partner"]')
  #headOfFamily = this.page.locator('[data-qa="head-of-family"]')

  async assertPartnerName(expectedName: string) {
    await waitUntilEqual(() => this.#partnerName.innerText(), expectedName)
  }

  async assertPartnerNameNotShown() {
    await this.#headOfFamily.waitFor()
    await this.#partnerName.waitFor({ state: 'hidden' })
  }
}

export class ValueDecisionsPage {
  constructor(private readonly page: Page) {}

  #valueDecisionListPage = this.page.locator(
    '[data-qa="voucher-value-decisions-page"]'
  )
  #valueDecisionDetailsPage = this.page.locator(
    '[data-qa="voucher-value-decision-page"]'
  )
  readonly #fromDateInput = this.page.locator(
    '[data-qa="value-decisions-start-date"] input'
  )
  readonly #toDateInput = this.page.locator(
    '[data-qa="value-decisions-end-date"] input'
  )
  readonly #dateCheckbox = this.page.locator(
    '[data-qa="value-decision-search-by-start-date"]'
  )
  #allValueDecisionsToggle = new CheckboxLocator(
    this.page.locator('[data-qa="toggle-all-decisions"]')
  )
  #sendValueDecisionsButton = new AsyncButton(
    this.page.locator('[data-qa="send-decisions"]')
  )
  #firstValueDecisionRow = this.page
    .locator('[data-qa="table-value-decision-row"]')
    .first()
  #navigateBackButton = this.page.locator('[data-qa="navigate-back"]')
  #statusFilter = {
    sent: new CheckboxLocator(
      this.page.locator('[data-qa="value-decision-status-filter-SENT"]')
    )
  }

  async openFirstValueDecision() {
    await this.#firstValueDecisionRow.click()
    await this.#valueDecisionDetailsPage.waitFor()
  }

  async navigateBackFromDetails() {
    await this.#navigateBackButton.click()
    await this.#valueDecisionListPage.waitFor()
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

  async startDateWithinRange() {
    await this.#dateCheckbox.click()
  }

  async toggleAllValueDecisions(toggledOn: boolean) {
    await this.#allValueDecisionsToggle.waitUntilChecked(!toggledOn)
    await this.#allValueDecisionsToggle.click()
    await this.#allValueDecisionsToggle.waitUntilChecked(toggledOn)
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
      () =>
        this.page.$$eval(
          '[data-qa="table-value-decision-row"]',
          (rows) => rows.length
        ),
      count
    )
  }
}

export class ValueDecisionDetailsPage {
  constructor(private readonly page: Page) {}

  #partnerName = this.page.locator('[data-qa="partner"]')
  #headOfFamily = this.page.locator('[data-qa="head-of-family"]')

  #sendDecisionButton = this.page.locator('[data-qa="button-send-decision"]')

  async sendValueDecision() {
    await this.#sendDecisionButton.click()
    await this.#sendDecisionButton.waitFor({ state: 'hidden' })
  }

  async assertPartnerName(expectedName: string) {
    await waitUntilEqual(() => this.#partnerName.innerText(), expectedName)
  }

  async assertPartnerNameNotShown() {
    await this.#headOfFamily.waitFor()
    await this.#partnerName.waitFor({ state: 'hidden' })
  }
}

export class InvoicesPage {
  constructor(private readonly page: Page) {}

  #invoicesPage = this.page.locator('[data-qa="invoices-page"]')
  #invoiceDetailsPage = this.page.locator('[data-qa="invoice-details-page"]')
  #spinner = this.page.locator('.loader-spinner')
  #createInvoicesButton = this.page.locator('[data-qa="create-invoices"]')
  #invoiceInList = this.page.locator('[data-qa="table-invoice-row"]')
  #allInvoicesToggle = new CheckboxLocator(
    this.page.locator('[data-qa="toggle-all-invoices"]')
  )
  #openSendInvoicesDialogButton = this.page.locator(
    '[data-qa="open-send-invoices-dialog"]'
  )
  #sendInvoicesDialog = this.page.locator('[data-qa="send-invoices-dialog"]')
  #sendInvoicesButton = new AsyncButton(
    this.page.locator(
      '[data-qa="send-invoices-dialog"] [data-qa="modal-okBtn"]'
    )
  )
  #navigateBack = this.page.locator('[data-qa="navigate-back"]')
  #invoiceDetailsHeadOfFamily = this.page.locator(
    '[data-qa="invoice-details-head-of-family"]'
  )
  #addInvoiceRowButton = this.page.locator('[data-qa="invoice-button-add-row"]')
  #invoiceRow = (index: number) => {
    const row = this.page.locator(
      `[data-qa="invoice-details-invoice-row"]:nth-child(${index + 1})`
    )
    return {
      costCenterInput: row.locator('[data-qa="input-cost-center"]'),
      amountInput: row.locator('[data-qa="input-amount"]'),
      unitPriceInput: row.locator('[data-qa="input-price"]'),
      deleteRowButton: row.locator('[data-qa="delete-invoice-row-button"]')
    }
  }
  #saveChangesButton = new AsyncButton(
    this.page.locator('[data-qa="invoice-actions-save-changes"]')
  )
  #markInvoiceSentButton = new AsyncButton(
    this.page.locator('[data-qa="invoice-actions-mark-sent"]')
  )

  async invoicesPageIsLoaded() {
    await this.#invoicesPage.waitFor()
    await this.#spinner.waitFor({ state: 'hidden' })
  }

  async createInvoiceDrafts() {
    await this.#createInvoicesButton.click()
    await this.#spinner.waitFor({ state: 'hidden' })
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
    await this.#allInvoicesToggle.waitUntilChecked(!toggled)
    await this.#allInvoicesToggle.click()
    await this.#allInvoicesToggle.waitUntilChecked(toggled)
  }

  async sendInvoices() {
    await this.#openSendInvoicesDialogButton.click()
    await this.#sendInvoicesDialog.waitFor()
    await this.#sendInvoicesButton.click()
    await this.#sendInvoicesButton.waitUntilSuccessful()
  }

  async showSentInvoices() {
    await this.page.locator('[data-qa="invoice-status-filter-SENT"]').click()
  }

  async showWaitingForSendingInvoices() {
    await this.page
      .locator('[data-qa="invoice-status-filter-WAITING_FOR_SENDING"]')
      .click()
  }

  async openFirstInvoice() {
    await this.#invoiceInList.click()
    await this.#invoiceDetailsPage.waitFor()
  }

  async assertInvoiceHeadOfFamily(fullName: string) {
    await this.#invoiceDetailsPage.waitFor()
    await waitUntilEqual(
      () => this.#invoiceDetailsHeadOfFamily.innerText(),
      fullName
    )
  }

  async navigateBackToInvoices() {
    await this.#navigateBack.click()
    await this.#invoicesPage.waitFor()
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
      () =>
        this.#invoiceInList.locator('[data-qa="invoice-total"]').innerText(),
      this.formatFinnishDecimal(total)
    )
  }

  async freeTextFilter(text: string) {
    await this.page.locator('[data-qa="free-text-search-input"]').type(text)
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

  #incomeStatementRow = this.page.locator(`[data-qa="income-statement-row"]`)

  async getRowCount(): Promise<number> {
    return this.#incomeStatementRow.count()
  }

  async openNthIncomeStatement(nth: number) {
    await this.#incomeStatementRow.nth(nth).locator('a').click()
  }
}
