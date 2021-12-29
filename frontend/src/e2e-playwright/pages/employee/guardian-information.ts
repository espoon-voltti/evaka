// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../../e2e-test-common/config'
import { waitUntilEqual } from '../../utils'
import LocalDate from 'lib-common/local-date'
import { Checkbox, Element, Page } from '../../utils/page'
import { IncomeStatementPage } from './IncomeStatementPage'
import { UUID } from 'lib-common/types'

export default class GuardianInformationPage {
  constructor(private readonly page: Page) {}

  async navigateToGuardian(personId: UUID) {
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

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector, section } = collapsibles[collapsible]
    const element = this.page.find(selector)
    await element.click()
    return new section(this.page, element) as SectionFor<C>
  }
}

class Section extends Element {
  constructor(protected page: Page, root: Element) {
    super(root)
  }
}

class IncomesSection extends Section {
  #incomeStatementRows = this.findAll(`[data-qa="income-statement-row"]`)

  async isIncomeStatementHandled(nth = 0) {
    return new Checkbox(
      this.#incomeStatementRows.nth(nth).find(`[data-qa="is-handled-checkbox"]`)
    ).checked
  }

  async openIncomeStatement(nth = 0) {
    await this.#incomeStatementRows.nth(nth).find('a').click()
    return new IncomeStatementPage(this.page)
  }

  async getIncomeStatementInnerText(nth = 0) {
    return this.#incomeStatementRows.nth(nth).innerText
  }
}

class FeeDecisionsSection extends Section {
  #feeDecisionSentAt = this.findAll(`[data-qa="fee-decision-sent-at"]`)

  async checkFeeDecisionSentAt(nth: number, expectedSentAt: LocalDate) {
    await waitUntilEqual(
      () => this.#feeDecisionSentAt.nth(nth).innerText,
      expectedSentAt.format('dd.MM.yyyy')
    )
  }
}

const collapsibles = {
  incomes: {
    selector: '[data-qa="person-income-collapsible"]',
    section: IncomesSection
  },
  feeDecisions: {
    selector: '[data-qa="person-fee-decisions-collapsible"]',
    section: FeeDecisionsSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
