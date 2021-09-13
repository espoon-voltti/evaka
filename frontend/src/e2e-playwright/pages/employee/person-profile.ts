// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

type ProfilePageCollapsible = 'person-income'

export class PersonProfilePage {
  constructor(private readonly page: Page) {}

  async openCollapsible(selector: ProfilePageCollapsible) {
    const locator = this.page.locator(`[data-qa="${selector}-collapsible"]`)
    if ((await locator.getAttribute('data-status')) === 'closed') {
      await locator.locator('[data-qa="collapsible-trigger"]').click()
    }
  }

  async setIncomeStatementHandled(nth = 1) {
    await this.page
      .locator(`[data-qa="income-statement-row"]`)
      .nth(nth)
      .locator(`[data-qa="set-handled-checkbox"]`)
      .click()
  }
}
