// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator, Page } from 'playwright'

export default class FridgeHeadInformationPage {
  constructor(private readonly page: Page) {}

  readonly incomesCollapsible = this.page.locator(
    '[data-qa="person-income-collapsible"]'
  )

  async openIncomesCollapsible() {
    await this.openCollapsible(this.incomesCollapsible)
  }

  async openCollapsible(collapsibleSelector: Locator) {
    if ((await collapsibleSelector.getAttribute('data-status')) === 'closed') {
      await collapsibleSelector
        .locator('[data-qa="collapsible-trigger"]')
        .click()
    }
  }

  incomesSection() {
    return new IncomesSection(this.page)
  }
}

export class IncomesSection {
  constructor(public page: Page) {}

  #newIncomeButton = this.page.locator('[data-qa="add-income-button"]')

  async openNewIncomeForm() {
    await this.#newIncomeButton.click()
  }

  #incomeDateRange = this.page.locator('[data-qa="income-date-range"]')
  #incomeStartDateInput = this.#incomeDateRange.locator(
    '[data-qa="date-range-input-start-date"] input'
  )
  #incomeEndDateInput = this.#incomeDateRange.locator(
    '[data-qa="date-range-input-end-date"] input'
  )

  async fillIncomeStartDate(value: string) {
    await this.#incomeStartDateInput.fill(value)
  }

  async fillIncomeEndDate(value: string) {
    await this.#incomeEndDateInput.fill(value)
  }

  #incomeInput = (type: string) =>
    this.page.locator(`[data-qa="income-input-${type}"]`)

  async fillIncome(type: string, value: string) {
    await this.#incomeInput(type).fill(value)
  }

  #incomeEffect = (effect: string) =>
    this.page.locator(`[data-qa="income-effect-${effect}"]`)

  async chooseIncomeEffect(effect: string) {
    await this.#incomeEffect(effect).click()
  }

  #coefficientSelect = (type: string) =>
    this.page.locator(`[data-qa="income-coefficient-select-${type}"] select`)

  async chooseCoefficient(type: string, coefficient: string) {
    await this.#coefficientSelect(type).selectOption({ value: coefficient })
  }

  #saveIncomeButton = this.page.locator('[data-qa="save-income"]')

  async save() {
    await this.#saveIncomeButton.click()
  }

  async saveIsDisabled() {
    return await this.#saveIncomeButton.isDisabled()
  }

  #incomeListItem = this.page.locator('[data-qa="income-list-item"]')

  async incomeListItemCount() {
    return await this.#incomeListItem.count()
  }

  #toggleIncomeItemButton = this.page.locator('[data-qa="toggle-income-item"]')

  async toggleIncome() {
    await this.#toggleIncomeItemButton.click()
  }

  #incomeSum = this.page.locator('[data-qa="income-sum-income"]')

  async getIncomeSum() {
    return await this.#incomeSum.textContent()
  }

  #expensesSum = this.page.locator('[data-qa="income-sum-expenses"]')

  async getExpensesSum() {
    return await this.#expensesSum.textContent()
  }

  #editIncomeItemButton = this.page.locator('[data-qa="edit-income-item"]')

  async edit() {
    await this.#editIncomeItemButton.click()
  }
}
