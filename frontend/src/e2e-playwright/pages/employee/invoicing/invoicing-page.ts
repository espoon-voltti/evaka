// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { RawElement, RawTextInput } from 'e2e-playwright/utils/element'
import LocalDate from 'lib-common/local-date'

export default class InvoicingPage {
  constructor(private readonly page: Page) {}

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

  async selectTab(tab: 'fee-decisions' | 'value-decisions' | 'invoices') {
    const elem = new RawElement(this.page, `[data-qa="${tab}-tab"]`)
    await elem.click()
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
}
