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
import { waitUntilEqual, waitUntilTrue } from 'e2e-playwright/utils'

export default class InvoicingPage {
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

  async selectTab(tab: 'fee-decisions' | 'value-decisions' | 'invoices') {
    const elem = new RawElement(this.page, `[data-qa="${tab}-tab"]`)
    await elem.click()
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
    await waitUntilEqual(
      () => this.#sendValueDecisionsButton.status(),
      'success'
    )
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
