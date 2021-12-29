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
      .find('[data-qa="person-info-section"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa="family-overview-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector } = collapsibles[collapsible]
    const element = this.page.find(selector)
    await element.click()
    return this.getCollapsible(collapsible)
  }

  getCollapsible<C extends Collapsible>(collapsible: C): SectionFor<C> {
    const { selector, section } = collapsibles[collapsible]
    const element = this.page.find(selector)
    return new section(this.page, element) as SectionFor<C>
  }
}

class Section extends Element {
  constructor(protected page: Page, root: Element) {
    super(root)
  }
}

class PersonInfoSection extends Section {
  #lastName = this.find('[data-qa="person-last-name"]')
  #firstName = this.find('[data-qa="person-first-names"]')
  #ssn = this.find('[data-qa="person-ssn"]')

  async assertPersonInfo(lastName: string, firstName: string, ssn: string) {
    await this.#lastName.findText(lastName).waitUntilVisible()
    await this.#firstName.findText(firstName).waitUntilVisible()
    await this.#ssn.findText(ssn).waitUntilVisible()
  }
}

class DependantsSection extends Section {
  #dependantChildren = this.find('[data-qa="table-of-dependants"]')

  async assertContainsDependantChild(childName: string) {
    await this.#dependantChildren.findText(childName).waitUntilVisible()
  }
}

class ApplicationsSection extends Section {
  #applicationRows = this.findAll('[data-qa="table-application-row"]')

  async assertApplicationCount(n: number) {
    await waitUntilEqual(() => this.#applicationRows.count(), n)
  }

  async assertApplicationSummary(
    n: number,
    childName: string,
    unitName: string
  ) {
    const row = this.#applicationRows.nth(n)
    await row.findText(childName).waitUntilVisible()
    await row.findText(unitName).waitUntilVisible()
  }
}

class DecisionsSection extends Section {
  #decisionRows = this.findAll('[data-qa="table-decision-row"]')

  async assertDecisionCount(n: number) {
    await waitUntilEqual(() => this.#decisionRows.count(), n)
  }

  async assertDecision(
    n: number,
    childName: string,
    unitName: string,
    status: string
  ) {
    const row = this.#decisionRows.nth(n)
    await row.findText(childName).waitUntilVisible()
    await row.findText(unitName).waitUntilVisible()
    await row.findText(status).waitUntilVisible()
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

class InvoicesSection extends Section {
  #invoiceRows = this.findAll('[data-qa="table-invoice-row"]')

  async assertInvoiceCount(n: number) {
    await waitUntilEqual(() => this.#invoiceRows.count(), n)
  }

  async assertInvoice(
    n: number,
    startDate: string,
    endDate: string,
    status: string
  ) {
    const row = this.#invoiceRows.nth(0)
    await row.findText(startDate).waitUntilVisible()
    await row.findText(endDate).waitUntilVisible()
    await row.findText(status).waitUntilVisible()
  }
}

const collapsibles = {
  personInfo: {
    selector: '[data-qa="person-info-collapsible"]',
    section: PersonInfoSection
  },
  dependants: {
    selector: '[data-qa="person-dependants-collapsible"]',
    section: DependantsSection
  },
  applications: {
    selector: '[data-qa="person-applications-collapsible"]',
    section: ApplicationsSection
  },
  decisions: {
    selector: '[data-qa="person-decisions-collapsible"]',
    section: DecisionsSection
  },
  incomes: {
    selector: '[data-qa="person-income-collapsible"]',
    section: IncomesSection
  },
  feeDecisions: {
    selector: '[data-qa="person-fee-decisions-collapsible"]',
    section: FeeDecisionsSection
  },
  invoices: {
    selector: '[data-qa="person-invoices-collapsible"]',
    section: InvoicesSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
