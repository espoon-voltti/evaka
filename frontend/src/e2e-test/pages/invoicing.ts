// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'
import config from 'e2e-test-common/config'
import { Checkbox, scrollThenClick } from '../utils/helpers'

export default class InvoicingPage {
  readonly url = config.employeeUrl

  readonly loginBtn = Selector('[data-qa="login-btn"]')
  readonly devLoginSubmitBtn = Selector('form button')
  readonly userNameBtn = Selector('[data-qa="username"]')
  readonly logoutBtn = Selector('[data-qa="logout-btn"]')

  readonly financeNav = Selector('[data-qa="finance-nav"]')
  readonly feeDecisionsTab = Selector('[data-qa="fee-decisions-tab"]')
  readonly valueDecisionsTab = Selector('[data-qa="value-decisions-tab"]')
  readonly invoicesTab = Selector('[data-qa="invoices-tab"]')
  readonly navigateBack = Selector('[data-qa="navigate-back"]')

  readonly tabNavigation = Selector('[data-qa="tab-navigation"]')
  readonly draftsTab = Selector('[data-qa="tab-navigation-drafts"]')
  readonly allTab = Selector('[data-qa="tab-navigation-all"]')

  readonly feeDecisionsPage = Selector('[data-qa="fee-decisions-page"]')
  readonly feeDecisionTable = Selector('[data-qa="table-of-decisions"]')
  readonly feeDecisionRows = Selector('[data-qa="table-fee-decision-row"]')
  readonly firstFeeDecisionRow = this.feeDecisionRows.nth(0)
  readonly toggleAllFeeDecisions = new Checkbox(
    Selector('[data-qa="toggle-all-decisions"]')
  )
  readonly toggleFirstFeeDecision = new Checkbox(
    this.firstFeeDecisionRow.find('[data-qa="toggle-decision"]')
  )
  readonly confirmFeeDecisions = Selector('[data-qa="confirm-decisions"]')
  readonly feeDecisionsStatusFilterDraft = new Checkbox(
    Selector('[data-qa="fee-decision-status-filter-DRAFT"]')
  )
  readonly feeDecisionsStatusFilterWaitingForSending = new Checkbox(
    Selector('[data-qa="fee-decision-status-filter-WAITING_FOR_SENDING"]')
  )
  readonly feeDecisionsStatusFilterSent = new Checkbox(
    Selector('[data-qa="fee-decision-status-filter-SENT"]')
  )

  readonly feeDecisionDetailsPage = Selector(
    '[data-qa="fee-decision-details-page"]'
  )

  readonly valueDecisionsPage = Selector(
    '[data-qa="voucher-value-decisions-page"]'
  )
  readonly valueDecisionTable = Selector('[data-qa="table-of-decisions"]')
  readonly valueDecisionRows = Selector('[data-qa="table-value-decision-row"]')
  readonly firstValueDecisionRow = this.valueDecisionRows.nth(0)
  readonly toggleAllValueDecisions = new Checkbox(
    Selector('[data-qa="toggle-all-decisions"]')
  )
  readonly toggleFirstValueDecision = new Checkbox(
    this.firstFeeDecisionRow.find('[data-qa="toggle-decision"]')
  )
  readonly sendValueDecisions = Selector('[data-qa="send-decisions"]')
  readonly valueDecisionsStatusFilterDraft = new Checkbox(
    Selector('[data-qa="value-decision-status-filter-DRAFT"]')
  )
  readonly valueDecisionsStatusFilterWaitingForSending = new Checkbox(
    Selector('[data-qa="value-decision-status-filter-WAITING_FOR_SENDING"]')
  )
  readonly valueDecisionsStatusFilterSent = new Checkbox(
    Selector('[data-qa="value-decision-status-filter-SENT"]')
  )
  readonly valueDecisionPage = Selector(
    '[data-qa="voucher-value-decision-page"]'
  )
  readonly areaFilter = (areaShortName: string) =>
    Selector(`[data-qa="area-filter-${areaShortName}"]`)

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
  readonly invoiceCreatedAt = Selector('[data-qa="invoice-created-at"]')

  readonly loaderSpinner = Selector('.loader-spinner')

  async login(t: TestController) {
    await t.click(this.loginBtn)
    await t.click(this.devLoginSubmitBtn)
  }

  async navigateToFeeDecisions(t: TestController) {
    await t.click(this.financeNav)
    await t.click(this.feeDecisionsTab)
  }

  async navigateToValueDecisions(t: TestController) {
    await t.click(this.financeNav)
    await t.click(this.valueDecisionsTab)
  }

  async navigateToInvoices(t: TestController) {
    await t.click(this.financeNav)
    await t.click(this.invoicesTab)
  }

  async openFirstFeeDecision(t: TestController) {
    await scrollThenClick(t, this.firstFeeDecisionRow)
  }

  async openFirstValueDecision(t: TestController) {
    await scrollThenClick(t, this.firstValueDecisionRow)
  }

  async confirmAllFeeDecisions(t: TestController) {
    await this.toggleAllFeeDecisions.click()
    await t.click(this.confirmFeeDecisions)
  }

  async openFirstInvoice(t: TestController) {
    await t.expect(this.loaderSpinner.exists).notOk()
    await scrollThenClick(t, this.firstInvoiceRow)
  }
}
