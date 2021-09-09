// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

export default class CitizenIncomePage {
  constructor(private readonly page: Page) {}

  rows = this.page.locator('tbody tr')

  async createNewIncomeStatement() {
    await this.page.locator('[data-qa="new-income-statement-btn"]').click()
  }

  async selectIncomeStatementType(
    type: 'highest-fee' | 'gross-income' | 'entrepreneur-income'
  ) {
    await this.page.locator(`[data-qa="${type}-checkbox"]`).click()
  }

  #startDate = this.page.locator('#start-date')
  async setValidFromDate(date: string) {
    await this.#startDate.selectText()
    await this.#startDate.type(date)
  }

  async submit() {
    await this.page.locator('button.primary').click()
  }

  async checkAssured() {
    await this.page.locator('[data-qa="assure-checkbox"]').click()
  }
}
