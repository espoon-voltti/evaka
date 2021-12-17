// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../../e2e-test-common/config'
import { waitUntilEqual } from '../../utils'
import LocalDate from 'lib-common/local-date'
import { Checkbox, Page } from '../../utils/page'

type ProfilePageCollapsible = 'person-income' | 'person-fee-decisions'

export class PersonProfilePage {
  constructor(private readonly page: Page) {}

  #incomeStatementRows = this.page.findAll(`[data-qa="income-statement-row"]`)
  #feeDecisionSentAt = this.page.findAll(`[data-qa="fee-decision-sent-at"]`)

  async openPersonPage(personId: string) {
    await this.page.goto(config.employeeUrl + '/profile/' + personId)
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="person-fridge-head-section"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa="family-overview-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async openCollapsible(selector: ProfilePageCollapsible) {
    const find = this.page.find(`[data-qa="${selector}-collapsible"]`)
    if ((await find.getAttribute('data-status')) === 'closed') {
      await find.find('[data-qa="collapsible-trigger"]').click()
    }
  }

  async isIncomeStatementHandled(nth = 0) {
    return new Checkbox(
      this.#incomeStatementRows.nth(nth).find(`[data-qa="is-handled-checkbox"]`)
    ).checked
  }

  async openIncomeStatement(nth = 0) {
    await this.#incomeStatementRows.nth(nth).find('a').click()
  }

  async getIncomeStatementInnerText(nth = 0) {
    return this.#incomeStatementRows.nth(nth).innerText
  }

  async checkFeeDecisionSentAt(nth: number, expectedSentAt: LocalDate) {
    await waitUntilEqual(
      () => this.#feeDecisionSentAt.nth(nth).innerText,
      expectedSentAt.format('dd.MM.yyyy')
    )
  }
}
