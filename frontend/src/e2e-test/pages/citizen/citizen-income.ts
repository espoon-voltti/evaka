// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Checkbox, Page, Radio, TextInput } from '../../utils/page'

export default class CitizenIncomePage {
  constructor(private readonly page: Page) {}

  rows = this.page.findAll('tbody tr')
  requiredAttachments = this.page.findByDataQa('required-attachments')
  assureCheckBox = new Checkbox(this.page.findByDataQa('assure-checkbox'))

  async createNewIncomeStatement() {
    await this.page.findByDataQa('new-income-statement-btn').click()
  }

  async selectIncomeStatementType(
    type: 'highest-fee' | 'gross-income' | 'entrepreneur-income'
  ) {
    await this.page.findByDataQa(`${type}-checkbox`).click()
  }

  #startDate = new TextInput(this.page.find('#start-date'))

  async setValidFromDate(date: string) {
    await this.#startDate.fill(date)
    await this.#startDate.press('Enter')
  }

  async submit() {
    await this.page.find('button.primary').click()
  }

  async checkAssured() {
    await this.assureCheckBox.check()
  }

  async checkIncomesRegisterConsent() {
    await this.page
      .find('[data-qa="incomes-register-consent-checkbox"]')
      .click()
  }

  async selectEntrepreneurType(value: 'full-time' | 'part-time') {
    await this.page.findByDataQa(`entrepreneur-${value}-option`).click()
  }

  #entrepreneurDate = new TextInput(
    this.page.findByDataQa('entrepreneur-start-date')
  )

  async setEntrepreneurStartDate(date: string) {
    await this.#entrepreneurDate.fill(date)
    await this.#entrepreneurDate.press('Enter')
  }

  async selectEntrepreneurSpouse(yesNo: 'yes' | 'no') {
    await new Radio(
      this.page.findByDataQa(`entrepreneur-spouse-${yesNo}`)
    ).check()
  }

  private async toggleCheckbox(
    checkbox:
      | 'student'
      | 'alimony-payer'
      | 'entrepreneur-startup-grant'
      | 'entrepreneur-checkup-consent'
      | 'entrepreneur-llc'
      | 'entrepreneur-light-entrepreneur'
      | 'entrepreneur-self-employed'
      | 'entrepreneur-partnership'
      | 'self-employed-attachments'
      | 'self-employed-estimated-income',
    check: boolean
  ) {
    const elem = new Checkbox(this.page.findByDataQa(`${checkbox}`))
    check ? await elem.check() : await elem.uncheck()
  }

  async toggleEntrepreneurStartupGrant(check: boolean) {
    await this.toggleCheckbox('entrepreneur-startup-grant', check)
  }

  async toggleEntrepreneurCheckupConsent(check: boolean) {
    await this.toggleCheckbox('entrepreneur-checkup-consent', check)
  }

  async toggleLimitedLiabilityCompany(check: boolean) {
    await this.toggleCheckbox('entrepreneur-llc', check)
  }

  async toggleLlcType(value: 'attachments' | 'incomes-register') {
    await this.page.findByDataQa(`llc-${value}`).click()
  }

  async toggleLightEntrepreneur(check: boolean) {
    await this.toggleCheckbox('entrepreneur-light-entrepreneur', check)
  }

  async toggleSelfEmployed(check: boolean) {
    await this.toggleCheckbox('entrepreneur-self-employed', check)
  }

  async togglePartnership(check: boolean) {
    await this.toggleCheckbox('entrepreneur-partnership', check)
  }

  async toggleSelfEmployedEstimatedIncome(check: boolean) {
    await this.toggleCheckbox('self-employed-estimated-income', check)
  }

  async toggleSelfEmployedAttachments(check: boolean) {
    await this.toggleCheckbox('self-employed-attachments', check)
  }

  async toggleStudent(check: boolean) {
    await this.toggleCheckbox('student', check)
  }

  async toggleAlimonyPayer(check: boolean) {
    await this.toggleCheckbox('alimony-payer', check)
  }

  async fillAccountant() {
    await new TextInput(this.page.findByDataQa('accountant-name')).fill(
      'Kirjanpitäjä'
    )
    await new TextInput(this.page.findByDataQa('accountant-email')).fill(
      'foo@example.com'
    )
    await new TextInput(this.page.findByDataQa('accountant-phone')).fill(
      '0400123456'
    )
  }

  async setGrossIncomeEstimate(income: number) {
    await new TextInput(
      this.page.findByDataQa('gross-monthly-income-estimate')
    ).fill(String(income))
  }
}
