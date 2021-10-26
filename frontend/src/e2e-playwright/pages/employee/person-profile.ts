// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from '../../../e2e-test-common/config'
import { waitUntilEqual } from '../../utils'
import LocalDate from 'lib-common/local-date'

type ProfilePageCollapsible = 'person-income' | 'person-fee-decisions'

export class PersonProfilePage {
  constructor(private readonly page: Page) {}

  #incomeStatementRow = this.page.locator(`[data-qa="income-statement-row"]`)
  #feeDecisionSentAt = this.page.locator(`[data-qa="fee-decision-sent-at"]`)

  async openPersonPage(personId: string) {
    await this.page.goto(config.employeeUrl + '/profile/' + personId)
  }

  async openCollapsible(selector: ProfilePageCollapsible) {
    const locator = this.page.locator(`[data-qa="${selector}-collapsible"]`)
    if ((await locator.getAttribute('data-status')) === 'closed') {
      await locator.locator('[data-qa="collapsible-trigger"]').click()
    }
  }

  async isIncomeStatementHandled(nth = 0) {
    return this.#incomeStatementRow
      .nth(nth)
      .locator(`[data-qa="is-handled-checkbox-input"]`)
      .isChecked()
  }

  async openIncomeStatement(nth = 0) {
    await this.#incomeStatementRow.nth(nth).locator('a').click()
  }

  async getIncomeStatementInnerText(nth = 0) {
    return this.#incomeStatementRow.nth(nth).innerText()
  }

  async checkFeeDecisionSentAt(nth: number, expectedSentAt: LocalDate) {
    await waitUntilEqual(
      () => this.#feeDecisionSentAt.nth(nth).innerText(),
      expectedSentAt.format('dd.MM.yyyy')
    )
  }
}
