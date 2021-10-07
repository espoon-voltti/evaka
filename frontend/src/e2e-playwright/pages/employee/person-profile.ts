// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

type ProfilePageCollapsible = 'person-income'

export class PersonProfilePage {
  constructor(private readonly page: Page) {}

  #row = this.page.locator(`[data-qa="income-statement-row"]`)

  async openCollapsible(selector: ProfilePageCollapsible) {
    const locator = this.page.locator(`[data-qa="${selector}-collapsible"]`)
    if ((await locator.getAttribute('data-status')) === 'closed') {
      await locator.locator('[data-qa="collapsible-trigger"]').click()
    }
  }

  async isIncomeStatementHandled(nth = 0) {
    return this.#row
      .nth(nth)
      .locator(`[data-qa="is-handled-checkbox-input"]`)
      .isChecked()
  }

  async openIncomeStatement(nth = 0) {
    await this.#row.nth(nth).locator('a').click()
  }

  async getIncomeStatementInnerText(nth = 0) {
    return this.#row.nth(nth).innerText()
  }
}
