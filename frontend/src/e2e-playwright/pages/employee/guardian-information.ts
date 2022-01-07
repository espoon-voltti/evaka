// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import { waitUntilEqual, waitUntilNotEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  Combobox,
  DatePicker,
  DatePickerDeprecated,
  Element,
  Modal,
  Page,
  Select,
  TextInput
} from '../../utils/page'
import { IncomeStatementPage } from './IncomeStatementPage'

export default class GuardianInformationPage {
  constructor(private readonly page: Page) {}

  async navigateToGuardian(personId: UUID) {
    await this.page.goto(config.employeeUrl + '/profile/' + personId)
    await this.waitUntilLoaded()
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="person-info-section"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa="family-overview-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  #restrictedDetailsEnabledLabel = this.page.find(
    '[data-qa="restriction-details-enabled-label"]'
  )
  #personStreetAddress = this.page.find(
    '[data-qa="person-details-street-address"]'
  )

  async assertRestrictedDetails(enabled: boolean) {
    switch (enabled) {
      case true:
        await this.#restrictedDetailsEnabledLabel.waitUntilVisible()
        await waitUntilEqual(
          () => this.#personStreetAddress.innerText,
          'Osoite ei ole saatavilla turvakiellon vuoksi'
        )
        break
      default:
        await this.#restrictedDetailsEnabledLabel.waitUntilHidden()
        await waitUntilNotEqual(
          () => this.#personStreetAddress.innerText,
          'Osoite ei ole saatavilla turvakiellon vuoksi'
        )
    }
  }

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector } = collapsibles[collapsible]
    const element = this.page.find(selector)
    if ((await element.getAttribute('data-status')) === 'closed') {
      await element.click()
    }
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

class FamilyOverviewSection extends Section {
  async assertPerson({
    personId,
    age,
    incomeCents
  }: {
    personId: string
    age?: number
    incomeCents?: number
  }) {
    const person = this.find(
      `[data-qa="table-family-overview-row-${personId}"]`
    )
    await person.waitUntilVisible()

    if (age !== undefined) {
      const personAge = person.find('[data-qa="person-age"]')
      await waitUntilEqual(() => personAge.textContent, age.toString())
    }

    if (incomeCents !== undefined) {
      const personIncome = person.find('[data-qa="person-income-total"]')
      const expectedIncome = formatCents(incomeCents)
      await waitUntilEqual(
        async () => ((await personIncome.textContent) ?? '').split(' ')[0],
        expectedIncome
      )
    }
  }
}

class PartnersSection extends Section {
  #addPartnerButton = this.find('[data-qa="add-partner-button"]')

  async addPartner(partnerName: string, startDate: string) {
    await this.#addPartnerButton.click()
    const modal = new Modal(this.page.find('[data-qa="fridge-partner-modal"]'))

    const combobox = new Combobox(
      modal.find('[data-qa="fridge-partner-person-search"]')
    )
    await combobox.fillAndSelectFirst(partnerName)

    const startDatePicker = new DatePickerDeprecated(
      modal.find('[data-qa="fridge-partner-start-date"]')
    )
    await startDatePicker.fill(startDate)

    await modal.submit()
  }
}

class ChildrenSection extends Section {
  #addChildButton = this.find('[data-qa="add-child-button"]')

  async addChild(childName: string, startDate: string) {
    await this.#addChildButton.click()
    const modal = new Modal(this.page.find('[data-qa="fridge-child-modal"]'))

    const combobox = new Combobox(
      modal.find('[data-qa="fridge-child-person-search"]')
    )
    await combobox.fillAndSelectFirst(childName)

    const startDatePicker = new DatePickerDeprecated(
      modal.find('[data-qa="fridge-child-start-date"]')
    )
    await startDatePicker.fill(startDate)

    await modal.submit()
  }

  #childrenTableRow = this.findAll('[data-qa="table-fridge-child-row"]')

  async verifyChildAge(age: number) {
    const childAge = this.#childrenTableRow.nth(0).find('[data-qa="child-age"]')
    await waitUntilEqual(() => childAge.textContent, age.toString())
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

export class IncomesSection extends Section {
  // Income statements

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

  // Incomes
  #newIncomeButton = this.page.find('[data-qa="add-income-button"]')

  async openNewIncomeForm() {
    await this.#newIncomeButton.click()
  }

  #incomeDateRange = this.page.find('[data-qa="income-date-range"]')
  #incomeStartDateInput = new DatePickerDeprecated(
    this.#incomeDateRange.find('[data-qa="date-range-input-start-date"]')
  )
  #incomeEndDateInput = new DatePickerDeprecated(
    this.#incomeDateRange.find('[data-qa="date-range-input-end-date"]')
  )

  async fillIncomeStartDate(value: string) {
    await this.#incomeStartDateInput.fill(value)
  }

  async fillIncomeEndDate(value: string) {
    await this.#incomeEndDateInput.fill(value)
  }

  #incomeInput = (type: string) =>
    new TextInput(this.page.find(`[data-qa="income-input-${type}"]`))

  async fillIncome(type: string, value: string) {
    await this.#incomeInput(type).fill(value)
  }

  #incomeEffect = (effect: string) =>
    this.page.find(`[data-qa="income-effect-${effect}"]`)

  async chooseIncomeEffect(effect: string) {
    await this.#incomeEffect(effect).click()
  }

  #coefficientSelect = (type: string) =>
    new Select(this.page.find(`[data-qa="income-coefficient-select-${type}"]`))

  async chooseCoefficient(type: string, coefficient: string) {
    await this.#coefficientSelect(type).selectOption({ value: coefficient })
  }

  #saveIncomeButton = this.page.find('[data-qa="save-income"]')

  async save() {
    await this.#saveIncomeButton.click()
    await this.#saveIncomeButton.waitUntilHidden()
  }

  async saveFailing() {
    await this.#saveIncomeButton.click()
  }

  async saveIsDisabled() {
    return await this.#saveIncomeButton.disabled
  }

  #incomeListItems = this.page.findAll('[data-qa="income-list-item"]')

  async incomeListItemCount() {
    return await this.#incomeListItems.count()
  }

  #toggleIncomeItemButton = this.page.find('[data-qa="toggle-income-item"]')

  async toggleIncome() {
    await this.#toggleIncomeItemButton.click()
  }

  #incomeSum = this.page.find('[data-qa="income-sum-income"]')

  async getIncomeSum() {
    return await this.#incomeSum.textContent
  }

  #expensesSum = this.page.find('[data-qa="income-sum-expenses"]')

  async getExpensesSum() {
    return await this.#expensesSum.textContent
  }

  #editIncomeItemButton = this.page.find('[data-qa="edit-income-item"]')

  async edit() {
    await this.#editIncomeItemButton.click()
  }
}

class FeeDecisionsSection extends Section {
  #feeDecisionTableRows = this.findAll('tbody tr')
  #feeDecisionSentAt = this.findAll(`[data-qa="fee-decision-sent-at"]`)

  async assertFeeDecision(
    n: number,
    {
      startDate,
      endDate,
      status
    }: {
      startDate: string
      endDate: string
      status: string
    }
  ) {
    const decision = this.#feeDecisionTableRows.nth(n)
    await waitUntilTrue(async () =>
      ((await decision.textContent) ?? '').includes(
        `Maksupäätös ${startDate} - ${endDate}`
      )
    )
    await waitUntilTrue(async () =>
      ((await decision.textContent) ?? '').includes(status)
    )
  }

  async createRetroactiveFeeDecisions(date: string) {
    await this.find(
      '[data-qa="create-retroactive-fee-decision-button"]'
    ).click()
    const modal = new Modal(this.page.find('[data-qa="modal"]'))

    const startDate = new DatePicker(
      modal.find('[data-qa="retroactive-fee-decision-start-date"]')
    )
    await startDate.fill(date)

    await modal.submit()
  }

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
  familyOverview: {
    selector: '[data-qa="family-overview-collapsible"]',
    section: FamilyOverviewSection
  },
  partners: {
    selector: '[data-qa="person-partners-collapsible"]',
    section: PartnersSection
  },
  children: {
    selector: '[data-qa="person-children-collapsible"]',
    section: ChildrenSection
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
