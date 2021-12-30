// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  DatePickerDeprecated,
  Element,
  Page,
  Select,
  TextInput
} from 'e2e-playwright/utils/page'

export default class FridgeHeadInformationPage {
  constructor(private readonly page: Page) {}

  readonly incomesCollapsible = this.page.find(
    '[data-qa="person-income-collapsible"]'
  )

  async openIncomesCollapsible() {
    await this.openCollapsible(this.incomesCollapsible)
  }

  async openCollapsible(collapsibleSelector: Element) {
    if ((await collapsibleSelector.getAttribute('data-status')) === 'closed') {
      await collapsibleSelector.find('[data-qa="collapsible-trigger"]').click()
    }
  }

  incomesSection() {
    return new IncomesSection(this.page)
  }
}

export class IncomesSection {
  constructor(public page: Page) {}

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
