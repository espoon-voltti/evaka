// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'
import config from '../config'
import { Checkbox, scrollThenClick } from '../utils/helpers'

export default class InvoicingPage {
  readonly url = config.employeeUrl

  readonly loginBtn = Selector('[data-qa="login-btn"]')
  readonly devLoginSubmitBtn = Selector('form button')
  readonly logoutBtn = Selector('[data-qa="logout-btn"]')

  readonly decisionsNav = Selector('[data-qa="fee-decisions-nav"]')
  readonly invoicesNav = Selector('[data-qa="invoices-nav"]')
  readonly navigateBack = Selector('[data-qa="navigate-back"]')

  readonly tabNavigation = Selector('[data-qa="tab-navigation"]')
  readonly draftsTab = Selector('[data-qa="tab-navigation-drafts"]')
  readonly allTab = Selector('[data-qa="tab-navigation-all"]')

  readonly decisionsPage = Selector('[data-qa="fee-decisions-page"]')
  readonly decisionTable = Selector('[data-qa="table-of-decisions"]')
  readonly decisionRows = Selector('[data-qa="table-fee-decision-row"]')
  readonly firstDecisionRow = this.decisionRows.nth(0)
  readonly toggleAllDecisions = new Checkbox(
    Selector('[data-qa="toggle-all-decisions"]', { timeout: 50 })
  )
  readonly toggleFirstDecision = new Checkbox(
    this.firstDecisionRow.find('[data-qa="toggle-decision"]')
  )
  readonly confirmDecisions = Selector('[data-qa="confirm-decisions"]')
  readonly decisionsStatusFilterDraft = new Checkbox(
    Selector('[data-qa="fee-decision-status-filter-DRAFT"]')
  )
  readonly decisionsStatusFilterWaitingForSending = new Checkbox(
    Selector('[data-qa="fee-decision-status-filter-WAITING_FOR_SENDING"]')
  )
  readonly decisionsStatusFilterSent = new Checkbox(
    Selector('[data-qa="fee-decision-status-filter-SENT"]')
  )

  readonly decisionDetailsPage = Selector(
    '[data-qa="fee-decision-details-page"]'
  )
  readonly maxFeeAcceptedCheckbox = Selector(
    '[data-qa="checkbox-max-fee-accepted"]'
  )

  readonly invoicesPage = Selector('[data-qa="invoices-page"]')
  readonly invoiceTable = Selector('[data-qa="table-of-invoices"]')
  readonly invoiceRows = Selector('[data-qa="table-invoice-row"]')
  readonly firstInvoiceRow = this.invoiceRows.nth(0)
  readonly toggleAllInvoices = new Checkbox(
    Selector('[data-qa="toggle-all-invoices"]')
  )
  readonly toggleFirstInvoice = new Checkbox(
    this.firstInvoiceRow.find('[data-qa="toggle-invoice"]')
  )
  readonly createInvoices = Selector('[data-qa="create-invoices"]')
  readonly openInvoiceModal = Selector('[data-qa="open-send-invoices-dialog"]')
  readonly sendInvoices = Selector(
    '[data-qa="send-invoices-dialog"] [data-qa="modal-okBtn"]'
  )
  readonly invoicesStatusFilterDraft = new Checkbox(
    Selector('[data-qa="invoice-status-filter-DRAFT"]')
  )
  readonly invoicesStatusFilterWaitingForSending = new Checkbox(
    Selector('[data-qa="invoice-status-filter-WAITING_FOR_SENDING"]')
  )
  readonly invoicesStatusFilterSent = new Checkbox(
    Selector('[data-qa="invoice-status-filter-SENT"]')
  )
  readonly freeTextSearchInput = Selector('[data-qa="free-text-search-input"]')
  readonly invoiceDetailsPage = Selector('[data-qa="invoice-details-page"]')
  readonly saveInvoiceChangesButton = Selector(
    '[data-qa="invoice-actions-save-changes"]'
  )
  readonly markInvoiceSentButton = Selector(
    '[data-qa="invoice-actions-mark-sent"]'
  )
  readonly invoiceRow = Selector('[data-qa="invoice-details-invoice-row"]')
  readonly productSelect = Selector('[data-qa="select-product"]')
  readonly areaCodeSelect = Selector('[data-qa="select-area-code"]')
  readonly costCenterInput = Selector('[data-qa="input-cost-center"]')
  readonly subCostCenterSelect = Selector('[data-qa="select-sub-cost-center"]')
  readonly amountInput = Selector('[data-qa="input-amount"]')
  readonly priceInput = Selector('[data-qa="input-price"]')
  readonly addRowButton = Selector('[data-qa="invoice-button-add-row"]')
  readonly deleteRowButton = Selector('[data-qa="delete-invoice-row-button"]')

  readonly loaderSpinner = Selector('.loader-spinner')

  async login(t: TestController) {
    await t.click(this.loginBtn)
    await t.click(this.devLoginSubmitBtn)
  }

  async navigateToDecisions(t: TestController) {
    await t.click(this.decisionsNav)
  }

  async navigateToInvoices(t: TestController) {
    await t.click(this.invoicesNav)
    await t.expect(this.loaderSpinner.exists).notOk()
  }

  async openFirstDecision(t: TestController) {
    await scrollThenClick(t, this.firstDecisionRow)
  }

  async confirmAllDecisions(t: TestController) {
    await this.toggleAllDecisions.click()
    await t.click(this.confirmDecisions)
  }

  async openFirstInvoice(t: TestController) {
    await t.expect(this.loaderSpinner.exists).notOk()
    await scrollThenClick(t, this.firstInvoiceRow)
  }
}
